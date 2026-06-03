#!/usr/bin/env bash
# Kite statusline: model · context bar (color) · tokens · cost
# Reads Claude Code session JSON on stdin.
# Context size derived from the last usage record in transcript JSONL
# (Claude Code statusline payload does NOT carry token usage natively).

input=$(cat)

model=$(echo "$input" | jq -r '.model.display_name // .model.id // "Claude"')
model_id=$(echo "$input" | jq -r '.model.id // ""')
transcript_path=$(echo "$input" | jq -r '.transcript_path // ""')
cost=$(echo "$input" | jq -r '.cost.total_cost_usd // 0')
five_hour_perc=$(echo "$input" | jq -r '.rate_limits.five_hour.used_percentage // empty')
seven_day_perc=$(echo "$input" | jq -r '.rate_limits.seven_day.used_percentage // empty')

# Context-window detection.
# 9router can present a generic cb1[1m] marker for multiple upstream models;
# allow an env override while keeping explicit model-name mappings when exposed.
model_key=$(printf '%s %s' "$model_id" "$model" | tr '[:upper:]' '[:lower:]')
if [ -n "${KITE_STATUSLINE_CONTEXT_TOKENS:-}" ]; then
  total_tokens="$KITE_STATUSLINE_CONTEXT_TOKENS"
elif printf '%s' "$model_key" | grep -Eq 'gpt[- ]?5\.5|gpt5\.5'; then
  total_tokens=1000000
else
  case "$model_key" in
    *"[1m]"*|*"1m"*) total_tokens=1000000 ;;
    *) total_tokens=200000 ;;
  esac
fi
case "$total_tokens" in
  ''|*[!0-9]*) total_tokens=200000 ;;
esac

# Derive used_tokens from last assistant usage record in transcript
used_tokens=0
if [ -n "$transcript_path" ] && [ -r "$transcript_path" ]; then
  used_tokens=$(tac "$transcript_path" 2>/dev/null \
    | grep -m1 '"usage"' \
    | jq -r '
        (.message.usage // .usage // {}) as $u
        | (($u.input_tokens // 0) + ($u.cache_creation_input_tokens // 0) + ($u.cache_read_input_tokens // 0))
      ' 2>/dev/null)
  [ -z "$used_tokens" ] && used_tokens=0
fi

used_perc=$(awk -v u="$used_tokens" -v t="$total_tokens" 'BEGIN { if (t>0) printf "%d", (u/t)*100; else print 0 }')
[ "$used_perc" -lt 0 ] && used_perc=0
[ "$used_perc" -gt 100 ] && used_perc=100

if [ "$used_perc" -lt 70 ]; then
  color="\033[32m"   # green
elif [ "$used_perc" -lt 90 ]; then
  color="\033[33m"   # yellow
else
  color="\033[31m"   # red
fi
reset="\033[0m"
dim="\033[2m"
bold="\033[1m"
cyan="\033[36m"

# 10-segment progress bar
filled=$((used_perc / 10))
bar=""
for i in 1 2 3 4 5 6 7 8 9 10; do
  if [ "$i" -le "$filled" ]; then bar="${bar}█"; else bar="${bar}░"; fi
done

fmt_tokens() {
  awk -v n="$1" 'BEGIN {
    if (n+0 >= 1000) printf "%.1fk", n/1000;
    else printf "%d", n;
  }'
}
used_fmt=$(fmt_tokens "$used_tokens")
total_fmt=$(fmt_tokens "$total_tokens")
cost_fmt=$(awk -v c="$cost" 'BEGIN { printf "%.4f", c+0 }')

printf "${cyan}${bold}%s${reset} ${color}[%s] %d%%${reset} ${dim}%s/%s${reset} ${dim}\$%s${reset}" \
  "$model" "$bar" "$used_perc" "$used_fmt" "$total_fmt" "$cost_fmt"

# Rate-limit segment — only render if at least one field present
if [ -n "$five_hour_perc" ] || [ -n "$seven_day_perc" ]; then
  rl_color() {
    awk -v p="$1" 'BEGIN {
      n = p + 0
      if (n < 70) printf "\033[32m"
      else if (n < 90) printf "\033[33m"
      else printf "\033[31m"
    }'
  }
  five_disp="--"
  seven_disp="--"
  five_color="$dim"
  seven_color="$dim"
  if [ -n "$five_hour_perc" ]; then
    five_disp=$(awk -v p="$five_hour_perc" 'BEGIN { printf "%d%%", p+0 }')
    five_color=$(rl_color "$five_hour_perc")
  fi
  if [ -n "$seven_day_perc" ]; then
    seven_disp=$(awk -v p="$seven_day_perc" 'BEGIN { printf "%d%%", p+0 }')
    seven_color=$(rl_color "$seven_day_perc")
  fi
  printf " ${dim}5h:${reset}${five_color}%s${reset} ${dim}7d:${reset}${seven_color}%s${reset}" \
    "$five_disp" "$seven_disp"
fi
