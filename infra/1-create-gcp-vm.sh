#!/usr/bin/env bash
# =============================================================================
# 1-create-gcp-vm.sh
# Creates a GCP VM for the 4-agent Docker Sandboxes demo
#
# Prerequisites:
#   gcloud CLI installed and authenticated  (gcloud auth login)
#   A GCP project already created
#
# Usage:
#   chmod +x 1-create-gcp-vm.sh
#   ./1-create-gcp-vm.sh
#
# After the VM is created:
#   gcloud compute scp 2-setup-vm.sh demo-vm:~ --zone=us-central1-a
#   gcloud compute ssh demo-vm --zone=us-central1-a
#   bash ~/2-setup-vm.sh
# =============================================================================

set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
# Edit these to match your environment
PROJECT_ID="your-gcp-project-id"     # gcloud projects list
ZONE="us-central1-a"
VM_NAME="quarkus-insights-demo"

# n2-standard-16 = 16 vCPU, 64 GB RAM
# Comfortably runs 4 concurrent sandboxes (each with JVM + Postgres) + GNOME desktop
MACHINE_TYPE="n2-standard-16"

# 150 GB for OS + GNOME + Maven cache + 4 Docker daemons + build artefacts
DISK_SIZE="150GB"
DISK_TYPE="pd-ssd"

# Ubuntu 22.04 LTS — good Chrome Remote Desktop support and docker-sbx packages
IMAGE_FAMILY="ubuntu-2204-lts"
IMAGE_PROJECT="ubuntu-os-cloud"
# ──────────────────────────────────────────────────────────────────────────────

echo "==> Setting project: $PROJECT_ID"
gcloud config set project "$PROJECT_ID"

echo "==> Creating VM: $VM_NAME in $ZONE"
gcloud compute instances create "$VM_NAME" \
  --zone="$ZONE" \
  --machine-type="$MACHINE_TYPE" \
  --image-family="$IMAGE_FAMILY" \
  --image-project="$IMAGE_PROJECT" \
  --boot-disk-size="$DISK_SIZE" \
  --boot-disk-type="$DISK_TYPE" \
  --boot-disk-device-name="${VM_NAME}-disk" \
  --min-cpu-platform="Intel Cascade Lake" \
  --enable-nested-virtualization \
  --tags="demo-vm,https-server" \
  --metadata=enable-oslogin=true \
  --scopes="https://www.googleapis.com/auth/cloud-platform"

echo ""
echo "==> Firewall: allow IAP tunnel (SSH) and Chrome Remote Desktop"
# IAP SSH — already open by default in most projects; uncomment if needed:
# gcloud compute firewall-rules create allow-iap-ssh \
#   --direction=INGRESS --action=ALLOW --rules=tcp:22 \
#   --source-ranges=35.235.240.0/20 --target-tags=demo-vm

# Chrome Remote Desktop uses outbound connections only — no inbound rule needed.

echo ""
echo "==> Verify nested virtualisation is enabled (should print 'vmx' or 'svm')"
echo "    Run after the VM is up:"
echo "    gcloud compute ssh $VM_NAME --zone=$ZONE -- grep -Eo 'vmx|svm' /proc/cpuinfo | head -1"

echo ""
echo "==> Next steps:"
echo ""
echo "  1. Copy the setup script to the VM:"
echo "     gcloud compute scp 2-setup-vm.sh $VM_NAME:~ --zone=$ZONE"
echo ""
echo "  2. SSH in:"
echo "     gcloud compute ssh $VM_NAME --zone=$ZONE"
echo ""
echo "  3. Run the setup script inside the VM:"
echo "     bash ~/2-setup-vm.sh"
echo ""
echo "  4. Follow the Chrome Remote Desktop instructions printed at the end of setup."
echo ""
echo "Done. VM $VM_NAME created in $ZONE."
