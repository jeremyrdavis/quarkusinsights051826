#!/usr/bin/env bash
# =============================================================================
# 4-launch-agents.sh
# Day-of-demo script. Runs a pre-flight check, then launches all 4 sandboxes
# in branch mode using the demo tmux layout.
#
# Usage:  bash ~/4-launch-agents.sh
# =============================================================================

set -euo pipefail

REPO_DIR="${REPO:-$HOME/quarkusinsights051826}"

ok()   { echo -e "  \033[1;32m✓\033[0m  $*"; }
fail() { echo -e "  \033[1;31m✗\033[0m  $*"; PREFLIGHT_FAILED=1; }
warn() { echo -e "  \033[1;33m⚠\033[0m  $*"; }
PREFLIGHT_FAILED=0

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " Quarkus Insights — DDD Agent Demo  |  Pre-flight checklist"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# ── KVM ──────────────────────────────────────────────────────────────────────
if [ -e /dev/kvm ] && [ -r /dev/kvm ]; then
  ok "/dev/kvm readable (KVM available)"
else
  fail "/dev/kvm not accessible — sbx microVMs won't start. Did you reboot after setup?"
fi

# ── Docker ───────────────────────────────────────────────────────────────────
if docker info &>/dev/null; then
  ok "Docker daemon running"
else
  fail "Docker daemon not running. Try: sudo systemctl start docker"
fi

# ── sbx ──────────────────────────────────────────────────────────────────────
if command -v sbx &>/dev/null; then
  ok "sbx installed: $(sbx --version 2>/dev/null || echo '(version unknown)')"
else
  fail "sbx not found — run 2-setup-vm.sh"
fi

# ── sbx login ────────────────────────────────────────────────────────────────
# sbx stores credentials in ~/.docker/sbx — check for the token file
if [ -f "$HOME/.docker/sbx/credentials" ] || \
   [ -f "$HOME/.sbx/credentials" ] || \
   sbx ls &>/dev/null 2>&1; then
  ok "sbx authenticated"
else
  fail "sbx not authenticated — run: sbx login"
fi

# ── Agent CLIs ───────────────────────────────────────────────────────────────
for cli in claude codex copilot gemini; do
  if command -v "$cli" &>/dev/null 2>&1 || \
     npx --no "@google/gemini-cli" --version &>/dev/null 2>&1; then
    ok "$cli CLI found"
  else
    warn "$cli CLI not found — its sandbox may fail to initialise"
  fi
done

# ── API keys ─────────────────────────────────────────────────────────────────
check_key() {
  local var="$1" label="$2"
  if [ -n "${!var:-}" ]; then
    ok "$label set (${#!var} chars)"
  else
    fail "$label not set — export $var before launching"
  fi
}
check_key ANTHROPIC_API_KEY "ANTHROPIC_API_KEY"
check_key OPENAI_API_KEY    "OPENAI_API_KEY"
check_key GITHUB_TOKEN      "GITHUB_TOKEN (Copilot)"
check_key GEMINI_API_KEY    "GEMINI_API_KEY"

# ── Repo ─────────────────────────────────────────────────────────────────────
if [ -d "$REPO_DIR/.git" ]; then
  ok "Repo at $REPO_DIR"
else
  fail "Repo not found at $REPO_DIR"
fi

# ── SPEC.md ──────────────────────────────────────────────────────────────────
if [ -f "$REPO_DIR/SPEC.md" ] && [ -s "$REPO_DIR/SPEC.md" ]; then
  ok "SPEC.md present ($(wc -l < "$REPO_DIR/SPEC.md") lines)"
else
  fail "SPEC.md missing or empty — agents cannot start without it"
fi

# ── Agent instruction files ───────────────────────────────────────────────────
for f in \
  "$REPO_DIR/CLAUDE.md" \
  "$REPO_DIR/AGENTS.md" \
  "$REPO_DIR/.github/copilot-instructions.md" \
  "$REPO_DIR/GEMINI.md"; do
  if [ -s "$f" ]; then
    ok "$(basename "$f") / $(dirname "$f" | sed "s|$REPO_DIR/||")"
  else
    warn "$(basename "$f") missing or empty — run 3-adapt-skills.sh"
  fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$PREFLIGHT_FAILED" -ne 0 ]; then
  echo " ✗  Pre-flight failed. Fix the issues above before continuing."
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  exit 1
fi

echo " ✓  All checks passed. Launching agents..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# =============================================================================
# Clean up any leftover sandboxes from a previous run
# =============================================================================
for name in sandbox-claude sandbox-codex sandbox-copilot sandbox-gemini; do
  if sbx ls 2>/dev/null | grep -q "$name"; then
    echo "  Removing stale sandbox: $name"
    sbx rm "$name" 2>/dev/null || true
  fi
done

# =============================================================================
# Launch 4 sandboxes in branch mode via the tmux layout
# =============================================================================
cd "$REPO_DIR"

SESSION="agents"
tmux kill-session -t "$SESSION" 2>/dev/null || true
tmux new-session -d -s "$SESSION" -x 240 -y 60 -c "$REPO_DIR"
tmux rename-window -t "$SESSION:0" "agents"

# Four panes: top-left, bottom-left, top-right, bottom-right
tmux split-window -h  -t "$SESSION:0"
tmux split-window -v  -t "$SESSION:0.0"
tmux split-window -v  -t "$SESSION:0.1"

# Claude Code — top-left
tmux send-keys -t "$SESSION:0.0" \
  "sbx run claude --branch agent/claude-code --name sandbox-claude" Enter

# Gemini CLI — bottom-left
tmux send-keys -t "$SESSION:0.2" \
  "sbx run gemini --branch agent/gemini --name sandbox-gemini" Enter

# Codex — top-right
tmux send-keys -t "$SESSION:0.1" \
  "sbx run codex --branch agent/codex --name sandbox-codex" Enter

# Copilot — bottom-right
tmux send-keys -t "$SESSION:0.3" \
  "sbx run copilot --branch agent/copilot --name sandbox-copilot" Enter

echo ""
echo "  tmux session 'agents' started with 4 panes."
echo "  Attaching now... (Ctrl-b d to detach, Ctrl-b arrow to switch panes)"
echo ""
sleep 1
tmux attach-session -t "$SESSION"
