#!/usr/bin/env bash
#
# 既に作成済みのSlack Appの設定（manifest.json の内容）を更新するスクリプト。
# 例：ローカル検証用に作ったアプリを、本番ドメインのRedirect URLに切り替えたいときに使う。
#
# 使い方：
#   export SLACK_CONFIG_TOKEN=xoxe-....
#   ./update-slack-app.sh <APP_ID>
#
# <APP_ID> は create-slack-app.sh 実行時に表示された "App ID"（例: A0123ABCDEF）。
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANIFEST_PATH="${MANIFEST_PATH:-$SCRIPT_DIR/manifest.json}"

APP_ID="${1:-}"
if [ -z "$APP_ID" ]; then
  echo "使い方: ./update-slack-app.sh <APP_ID>"
  exit 1
fi

if [ -z "${SLACK_CONFIG_TOKEN:-}" ]; then
  echo "SLACK_CONFIG_TOKEN が設定されていません。"
  exit 1
fi

MANIFEST_JSON="$(cat "$MANIFEST_PATH")"

RESPONSE="$(curl -sS -X POST https://slack.com/api/apps.manifest.update \
  -H "Authorization: Bearer $SLACK_CONFIG_TOKEN" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d "$(python3 -c '
import json,sys
manifest = json.loads(sys.stdin.read())
print(json.dumps({"app_id": sys.argv[1], "manifest": manifest}))
' "$APP_ID" <<< "$MANIFEST_JSON")")"

OK="$(python3 -c 'import json,sys;print("true" if json.loads(sys.stdin.read()).get("ok") else "false")' <<< "$RESPONSE")"

if [ "$OK" != "true" ]; then
  echo "更新に失敗しました。Slackからのレスポンス："
  echo "$RESPONSE" | python3 -m json.tool
  exit 1
fi

echo "Slack App ($APP_ID) の設定を更新しました。"
echo "Client ID / Secret は変わりません（Redirect URLなどの設定のみ更新されます）。"
