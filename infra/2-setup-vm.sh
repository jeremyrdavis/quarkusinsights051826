#!/usr/bin/env bash
# =============================================================================
# 2-setup-vm.sh
# Run this inside the GCP VM after creation.
# Installs: GNOME desktop + Chrome Remote Desktop + Chrome,
#           KVM, Docker Engine, docker-sbx, JDK 21,
#           Claude Code, Codex CLI, GitHub Copilot CLI, Gemini CLI,
#           tmux + tilix (4-pane terminal for the live demo),
#           clones the demo repo, scaffolds Skills directories.
#
# Estimated run time: 10-15 min on n2-standard-16.
# =============================================================================

set -euo pipefail

REPO_URL="https://github.com/jeremyrdavis/quarkusinsights051826.git"
REPO_DIR="$HOME/quarkusinsights051826"

SKILL_REPOS=(
  "https://github.com/jeremyrdavis/quarkus-skill-testing.git"
  "https://github.com/jeremyrdavis/quarkus-skill-persistence.git"
  "https://github.com/jeremyrdavis/quarkus-skill-rest.git"
  "https://github.com/jeremyrdavis/quarkus-skill-logging.git"
)

log() { echo -e "\n\033[1;34m==> $*\033[0m"; }
warn() { echo -e "\033[1;33mWARN: $*\033[0m"; }

# =============================================================================
log "System update"
# =============================================================================
sudo apt-get update -qq
sudo apt-get upgrade -y -qq

# =============================================================================
log "GNOME desktop (minimal) + utilities"
# =============================================================================
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
  ubuntu-desktop-minimal \
  tilix \
  tmux \
  curl \
  wget \
  git \
  unzip \
  jq \
  htop \
  xdg-utils \
  ca-certificates \
  gnupg \
  lsb-release \
  apt-transport-https \
  software-properties-common

# =============================================================================
log "Chrome (required for Chrome Remote Desktop)"
# =============================================================================
wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb -O /tmp/chrome.deb
sudo dpkg -i /tmp/chrome.deb || sudo apt-get install -f -y -qq
rm /tmp/chrome.deb

# =============================================================================
log "Chrome Remote Desktop"
# =============================================================================
wget -q https://dl.google.com/linux/direct/chrome-remote-desktop_current_amd64.deb -O /tmp/crd.deb
sudo dpkg -i /tmp/crd.deb || sudo apt-get install -f -y -qq
rm /tmp/crd.deb

# Point Chrome Remote Desktop at the GNOME session
mkdir -p "$HOME/.config/chrome-remote-desktop"
cat > "$HOME/.chrome-remote-desktop-session" << 'EOF'
exec /etc/X11/Xsession /usr/bin/gnome-session
EOF
chmod +x "$HOME/.chrome-remote-desktop-session"

# =============================================================================
log "KVM — needed by docker-sbx microVM sandbox"
# =============================================================================
sudo apt-get install -y -qq qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils
sudo usermod -aG kvm "$USER"
sudo usermod -aG libvirt "$USER"

# Quick sanity check (non-fatal — nested virt must be enabled on the GCP VM)
if [ -e /dev/kvm ]; then
  echo "   /dev/kvm present ✓"
else
  warn "/dev/kvm not found — confirm nested virtualisation is enabled on the GCP VM."
fi

# =============================================================================
log "Docker Engine"
# =============================================================================
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update -qq
sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker "$USER"
sudo systemctl enable docker

# =============================================================================
log "docker-sbx (Docker Sandboxes CLI)"
# =============================================================================
# Add Docker's tap repo then install the sbx package
curl -fsSL https://get.docker.com | sudo REPO_ONLY=1 sh
sudo apt-get install -y -qq docker-sbx
# sbx also needs kvm group — already added above

# =============================================================================
log "JDK 21 (Temurin) — required by Quarkus demo app"
# =============================================================================
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/adoptium.gpg] \
  https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" \
  | sudo tee /etc/apt/sources.list.d/adoptium.list > /dev/null
sudo apt-get update -qq
sudo apt-get install -y -qq temurin-21-jdk

# =============================================================================
log "Node.js 22 (LTS) — needed by Claude Code, Codex, Copilot CLIs"
# =============================================================================
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash - > /dev/null
sudo apt-get install -y -qq nodejs

# =============================================================================
log "Agent CLIs"
# =============================================================================

# ── Claude Code ──────────────────────────────────────────────────────────────
echo "  Installing Claude Code..."
sudo npm install -g @anthropic-ai/claude-code 2>/dev/null

# ── Codex CLI (OpenAI) ───────────────────────────────────────────────────────
echo "  Installing Codex CLI..."
sudo npm install -g @openai/codex 2>/dev/null

# ── GitHub Copilot CLI ───────────────────────────────────────────────────────
echo "  Installing GitHub Copilot CLI..."
sudo npm install -g @githubnext/github-copilot-cli 2>/dev/null

# ── Gemini CLI ───────────────────────────────────────────────────────────────
echo "  Installing Gemini CLI..."
sudo npm install -g @google/gemini-cli 2>/dev/null

# =============================================================================
log "Clone demo repo"
# =============================================================================
if [ -d "$REPO_DIR" ]; then
  echo "  Repo already exists at $REPO_DIR — pulling latest"
  git -C "$REPO_DIR" pull --ff-only
else
  git clone "$REPO_URL" "$REPO_DIR"
fi

# =============================================================================
log "Fetch agent branches"
# =============================================================================
cd "$REPO_DIR"
git fetch --all
for branch in agent/claude-code agent/codex agent/copilot agent/gemini; do
  git checkout "$branch" 2>/dev/null || git checkout -b "$branch" origin/main 2>/dev/null || true
done
git checkout main

# =============================================================================
log "Clone Skills repos into $HOME/skills-source"
# =============================================================================
mkdir -p "$HOME/skills-source"
for repo in "${SKILL_REPOS[@]}"; do
  name=$(basename "$repo" .git)
  dest="$HOME/skills-source/$name"
  if [ -d "$dest" ]; then
    git -C "$dest" pull --ff-only
  else
    git clone "$repo" "$dest"
  fi
done

# =============================================================================
log "Scaffold Skills directories inside the demo repo"
# =============================================================================
# Claude Code — reads .claude/skills/<name>/SKILL.md
CLAUDE_SKILLS="$REPO_DIR/.claude/skills"
mkdir -p \
  "$CLAUDE_SKILLS/quarkus-skill-testing" \
  "$CLAUDE_SKILLS/quarkus-skill-persistence" \
  "$CLAUDE_SKILLS/quarkus-skill-rest" \
  "$CLAUDE_SKILLS/quarkus-skill-logging"

# Codex — reads AGENTS.md at repo root (inline) and also ~/.agents/skills/
AGENTS_SKILLS="$HOME/.agents/skills"
mkdir -p \
  "$AGENTS_SKILLS/quarkus-skill-testing" \
  "$AGENTS_SKILLS/quarkus-skill-persistence" \
  "$AGENTS_SKILLS/quarkus-skill-rest" \
  "$AGENTS_SKILLS/quarkus-skill-logging"

# Copilot — reads .github/copilot-instructions.md
mkdir -p "$REPO_DIR/.github"

# Gemini CLI — reads GEMINI.md at repo root
touch "$REPO_DIR/GEMINI.md"   # placeholder — populated by 3-adapt-skills.sh

echo "  Skill directory scaffolding done."
echo "  Run 3-adapt-skills.sh after your DDD Skills are complete to populate them."

# =============================================================================
log "tmux config for 4-agent demo layout"
# =============================================================================
cat > "$HOME/.tmux-demo.conf" << 'TMUXCONF'
# Quarkus Insights demo layout — 4 panes, one per agent
set -g mouse on
set -g default-terminal "screen-256color"

# Status bar
set -g status-bg colour235
set -g status-fg colour136
set -g status-left " #[bold]Quarkus Insights — DDD Agent Demo#[nobold] "
set -g status-left-length 50
set -g status-right " %H:%M "
TMUXCONF

cat > "$HOME/demo-layout.sh" << 'LAYOUT'
#!/usr/bin/env bash
# Starts a tmux session with 4 panes, one per agent sandbox
SESSION="agents"
REPO="$HOME/quarkusinsights051826"

tmux new-session -d -s "$SESSION" -x 220 -y 50 -c "$REPO"
tmux rename-window -t "$SESSION:0" "agents"

# Split into 4 panes: top-left, top-right, bottom-left, bottom-right
tmux split-window -h -t "$SESSION:0"
tmux split-window -v -t "$SESSION:0.0"
tmux split-window -v -t "$SESSION:0.1"

# Label each pane and pre-type the launch command (press Enter to start)
tmux send-keys -t "$SESSION:0.0" "# Claude Code  |  sbx run claude --branch agent/claude-code --name sandbox-claude" Enter
tmux send-keys -t "$SESSION:0.1" "# Codex        |  sbx run codex   --branch agent/codex       --name sandbox-codex"  Enter
tmux send-keys -t "$SESSION:0.2" "# Copilot      |  sbx run copilot --branch agent/copilot     --name sandbox-copilot" Enter
tmux send-keys -t "$SESSION:0.3" "# Gemini CLI   |  sbx run gemini  --branch agent/gemini      --name sandbox-gemini"  Enter

tmux attach-session -t "$SESSION"
LAYOUT
chmod +x "$HOME/demo-layout.sh"

# =============================================================================
log "Add environment variables to .bashrc"
# =============================================================================
cat >> "$HOME/.bashrc" << 'BASHRC'

# ── Quarkus Insights demo env ──────────────────────────────────────────────
export JAVA_HOME="/usr/lib/jvm/temurin-21-amd64"
export PATH="$JAVA_HOME/bin:$PATH"

# API keys — fill these in before the demo
# export ANTHROPIC_API_KEY=""
# export OPENAI_API_KEY=""
# export GITHUB_TOKEN=""
# export GEMINI_API_KEY=""

alias agents='bash ~/demo-layout.sh'
alias sbx-status='sbx ls'
BASHRC

# =============================================================================
log "Setup complete"
# =============================================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " SETUP COMPLETE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo " IMPORTANT — Reboot required for group memberships (kvm, docker) to apply:"
echo "   sudo reboot"
echo ""
echo " After reboot:"
echo "   1. Set up Chrome Remote Desktop via your browser:"
echo "      https://remotedesktop.google.com/headless"
echo "      Copy and run the Debian Linux command shown on that page."
echo ""
echo "   2. Add your API keys to ~/.bashrc:"
echo "      ANTHROPIC_API_KEY, OPENAI_API_KEY, GITHUB_TOKEN, GEMINI_API_KEY"
echo ""
echo "   3. Authenticate each agent CLI:"
echo "      claude auth login"
echo "      codex login"
echo "      github-copilot-cli auth"
echo "      gemini auth login"
echo ""
echo "   4. Authenticate sbx:"
echo "      sbx login"
echo ""
echo "   5. When your SPEC.md is ready, run the Skills adapter:"
echo "      bash ~/3-adapt-skills.sh"
echo ""
echo "   6. Launch the 4-agent demo:"
echo "      agents        (alias for ~/demo-layout.sh)"
echo ""
echo " Repo:   $REPO_DIR"
echo " Skills: $HOME/skills-source"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
