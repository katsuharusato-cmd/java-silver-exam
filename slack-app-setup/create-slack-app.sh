#!/usr/bin/env bash
#
# Slack App Manifest API を使って、Slack Appを自動作成するスクリプト。
#
# 事前に1回だけ必要な手動作業：
#   1. Slackにログインした状態で https://api.slack.com/authentication/config-tokens を開く
#   2. 対象のワークスペースを選び、「Generate Token」でアプリ設定トークン（xoxe-...）を発行してコピーする
#      （これはアプリを作るためのトークンで、Slack Appそのものではありません。1回発行すれば、
#        このワークスペース内のアプリ作成・更新すべてに使い回せます）
#
# 使い方：
#   export SLACK_CONFIG_TOKEN=xoxe-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
#   ./create-slack-app.sh
#
# 実行すると、slack-app-setup/manifest.json の内容でSlack Appが作成され、
# 発行されたClient ID / Client Secret を ../.env.slack に書き出します。
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANIFEST_PATH="${MANIFEST_PATH:-$SCRIPT_DIR/manifest.json}"
OUTPUT_ENV_FILE="${OUTPUT_ENV_FILE:-$SCRIPT_DIR/../.env.slack}"

if [ -z "${SLACK_CONFIG_TOKEN:-}" ]; then
  echo "SLACK_CONFIG_TOKEN が設定されていません。"
  echo "https://api.slack.com/authentication/config-tokens でアプリ設定トークンを発行し、"
  echo "  export SLACK_CONFIG_TOKEN=xoxe-...."
  echo "を実行してから、もう一度このスクリプトを実行してください。"
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 が見つかりません（レスポンス解析に使用します）。python3をインストールしてください。"
  exit 1
fi

if [ ! -f "$MANIFEST_PATH" ]; then
  echo "マニフェストファイルが見つかりません: $MANIFEST_PATH"
  exit 1
fi

echo "Slack Appを作成しています（manifest: $MANIFEST_PATH）..."

MANIFEST_JSON="$(cat "$MANIFEST_PATH")"

RESPONSE="$(curl -sS -X POST https://slack.com/api/apps.manifest.create \
  -H "Authorization: Bearer $SLACK_CONFIG_TOKEN" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d "$(python3 -c '
import json,sys
manifest = json.loads(sys.stdin.read())
print(json.dumps({"manifest": manifest}))
' <<< "$MANIFEST_JSON")")"

OK="$(python3 -c '
import json,sys
data = json.loads(sys.stdin.read())
print("true" if data.get("ok") else "false")
' <<< "$RESPONSE")"

if [ "$OK" != "true" ]; then
  echo "作成に失敗しました。Slackからのレスポンス："
  echo "$RESPONSE" | python3 -m json.tool
  exit 1
fi

APP_ID="$(python3 -c 'import json,sys;print(json.loads(sys.stdin.read())["app_id"])' <<< "$RESPONSE")"
CLIENT_ID="$(python3 -c 'import json,sys;print(json.loads(sys.stdin.read())["credentials"]["client_id"])' <<< "$RESPONSE")"
CLIENT_SECRET="$(python3 -c 'import json,sys;print(json.loads(sys.stdin.read())["credentials"]["client_secret"])' <<< "$RESPONSE")"

cat > "$OUTPUT_ENV_FILE" <<EOF
# create-slack-app.sh が自動生成しました。中身は公開リポジトリ等に絶対にコミットしないこと。
export SLACK_CLIENT_ID=$CLIENT_ID
export SLACK_CLIENT_SECRET=$CLIENT_SECRET
EOF

echo ""
echo "Slack Appを作成しました！"
echo "  App ID       : $APP_ID"
echo "  管理画面     : https://api.slack.com/apps/$APP_ID"
echo "  Client ID    : $CLIENT_ID"
echo "  Client Secret: (取得済み・$OUTPUT_ENV_FILE に書き出しました)"
echo ""
echo "次のコマンドでSpring Bootアプリを起動してください："
echo "  source $OUTPUT_ENV_FILE && mvn spring-boot:run"
echo ""
echo "※ Redirect URLは manifest.json に記載の"
echo "   http://localhost:8080/login/oauth2/code/slack"
echo "   で登録済みです。別ドメインで動かす場合は manifest.json を書き換えてから"
echo "   update-slack-app.sh で更新してください。"
