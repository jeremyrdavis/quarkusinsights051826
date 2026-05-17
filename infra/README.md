# infra/ — Live-Demo Infrastructure

Scripts that stand up the four-agent live-coding demo for the QuarkusInsights episode on DDD, Hexagonal Architecture, and agentic AI.

The demo runs **Claude Code, Codex, GitHub Copilot CLI, and Gemini CLI in parallel** — each inside its own [docker-sbx](https://github.com/docker/sandboxes) microVM — building the same Quarkus application from the same `SPEC.md`. The audience sees four side-by-side terminals comparing how each agent interprets the architectural rules in the project's Skills. See `../SHOW_NOTES.md` for the full episode plan.

These scripts are **for the stream host's machine, not for workshop attendees.** Attendees use `../workshop/`.

## What the scripts do, in order

| # | Script | Where to run | What it does |
|---|---|---|---|
| 1 | `1-create-gcp-vm.sh` | **Your laptop** | Creates an `n2-standard-16` GCP VM (16 vCPU, 64 GB) with nested virtualization for KVM, Ubuntu 22.04, SSD boot disk |
| 2 | `2-setup-vm.sh` | **Inside the VM** | Installs GNOME + Chrome Remote Desktop, KVM, Docker, docker-sbx, JDK, Node, the four agent CLIs, clones the demo repo, scaffolds Skills directories, writes a tmux 4-pane layout |
| 3 | `3-adapt-skills.sh` | **Inside the VM** | Reads `SKILL.md` files from `~/skills-source/` and emits them in each agent's native format: `.claude/skills/` for Claude Code, `~/.agents/skills/` + `AGENTS.md` for Codex, `.github/copilot-instructions.md` for Copilot, `GEMINI.md` for Gemini |
| 4 | `4-launch-agents.sh` | **Inside the VM, day of demo** | Runs a pre-flight (KVM readable, Docker up, sbx auth, API keys set, `SPEC.md` present) then launches four `sbx run` invocations in a 4-pane tmux session |

## Prerequisites

**For step 1 (your laptop):**

- `gcloud` CLI installed and authenticated (`gcloud auth login`)
- An existing GCP project — edit `PROJECT_ID` near the top of `1-create-gcp-vm.sh`
- Permission to create instances in `us-central1-a` (or change the zone)

**For step 4 (day of demo):**

- API keys exported in the VM's shell:
  - `ANTHROPIC_API_KEY` (Claude Code)
  - `OPENAI_API_KEY` (Codex)
  - `GITHUB_TOKEN` (Copilot)
  - `GEMINI_API_KEY` (Gemini)
- Each agent CLI authenticated (`claude auth login`, `codex login`, `github-copilot-cli auth`, `gemini auth login`)
- `sbx login` complete
- `SPEC.md` present and non-empty at the repo root — the agents read this as ground truth and won't ask clarifying questions

## How to run end-to-end

```bash
# On your laptop:
chmod +x infra/*.sh
./infra/1-create-gcp-vm.sh

# Copy the setup script up to the VM and SSH in:
gcloud compute scp infra/2-setup-vm.sh quarkus-insights-demo:~ --zone=us-central1-a
gcloud compute ssh quarkus-insights-demo --zone=us-central1-a

# Inside the VM (this takes 10–15 min):
bash ~/2-setup-vm.sh
sudo reboot   # required so kvm/docker group memberships take effect

# Reconnect, finish manual steps the setup script prints:
#   - Chrome Remote Desktop setup
#   - Authenticate each agent CLI
#   - Export API keys

# When your DDD Skills are ready, copy them into ~/skills-source/, then:
bash ~/3-adapt-skills.sh

# Day of demo (after SPEC.md is finalized):
bash ~/4-launch-agents.sh
```

## Things to know before running

- **`PROJECT_ID` is a placeholder.** `1-create-gcp-vm.sh` ships with `PROJECT_ID="your-gcp-project-id"` — set it to your real project ID or the `gcloud` call will fail loudly.

- **JDK version mismatch.** `2-setup-vm.sh` installs Temurin **21**. The current `quarkusinsightsddd/` project targets Java **25** (`maven.compiler.release=25` in its `pom.xml`). The demo's `SPEC.md` may or may not match — adjust the JDK install in `2-setup-vm.sh` (or install both) if you need 25.

- **Skills repos may not exist yet.** `2-setup-vm.sh` tries to clone `quarkus-skill-testing`, `quarkus-skill-persistence`, `quarkus-skill-rest`, `quarkus-skill-logging` from `github.com/jeremyrdavis/`. If they're not public yet, those clones fail silently and `3-adapt-skills.sh` skips them — that's fine; the scaffolding still works.

- **`docker-sbx` is in tech preview.** Installation via Docker's `get.docker.com` script then `apt-get install docker-sbx` may need adjustment as the package matures. If `sbx --version` fails after step 2, follow the current docker-sbx install docs and re-verify.

- **Nested virtualization is mandatory.** docker-sbx microVMs require KVM. `1-create-gcp-vm.sh` passes `--enable-nested-virtualization` and `--min-cpu-platform="Intel Cascade Lake"` for this. Don't change those without confirming KVM still works (`grep -Eo 'vmx|svm' /proc/cpuinfo` inside the VM).

- **Costs.** `n2-standard-16` runs ~$0.78/hr (us-central1, on-demand). The VM stays up between demos by default — `gcloud compute instances stop quarkus-insights-demo --zone=us-central1-a` when you're done.

- **The hardcoded repo URL** in `2-setup-vm.sh` (`quarkusinsights051826`) and `4-launch-agents.sh` matches the demo's GitHub remote. If you fork or rename, update both.

## Cleanup

```bash
# Stop the VM (preserves disk, no compute charges):
gcloud compute instances stop quarkus-insights-demo --zone=us-central1-a

# Or destroy it entirely:
gcloud compute instances delete quarkus-insights-demo --zone=us-central1-a
```
