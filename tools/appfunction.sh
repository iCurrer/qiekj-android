#!/usr/bin/env bash
#
# 小乖生活 · AppFunctions 调用工具
# =============================================================================
#
# 为什么需要它
# ------------
# AppFunctions 的「调用」有权限门槛：调用方必须持有
#   android.permission.EXECUTE_APP_FUNCTIONS   (prot=internal|privileged)
# 普通第三方 App 拿不到这个权限，所以函数做出来之后，能调它的只有两类：
#   1. 预置到系统分区的特权应用（各家厂商的 AI 助手）
#   2. adb shell（shell 用户持有该权限）
# 本脚本走第 2 条，用于开发和验证阶段。
#
# 用法
# ----
#   tools/appfunction.sh list                        列出系统索引到的全部函数
#   tools/appfunction.sh describe <函数名>            查看该函数的元数据原文
#   tools/appfunction.sh call <函数名> [JSON 参数]     调用函数（省略参数即 {}）
#
# 函数名支持简写，脚本会自动匹配完整的 functionId：
#   tools/appfunction.sh call queryBalance
#   tools/appfunction.sh call listDevices
#   tools/appfunction.sh call unlockDevice '{"deviceName":"1号楼-3层饮水机"}'
#
# 环境变量
# --------
#   ADB                  覆盖 adb 可执行文件路径（默认从 local.properties 推导）
#   APPFUNCTION_PACKAGE  覆盖包名（默认 ltd.fky.devicecontrol）
#   APPFUNCTION_TIMEOUT  调用超时秒数（默认 60）
#
# =============================================================================

set -euo pipefail

PKG="${APPFUNCTION_PACKAGE:-ltd.fky.devicecontrol}"
TIMEOUT="${APPFUNCTION_TIMEOUT:-60}"

# ---------------------------------------------------------------------------
# 定位 adb：环境变量 > local.properties 的 sdk.dir > PATH
# ---------------------------------------------------------------------------
find_adb() {
  if [[ -n "${ADB:-}" ]]; then
    printf '%s' "$ADB"; return
  fi
  local root props sdk cand
  root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
  props="$root/local.properties"
  if [[ -f "$props" ]]; then
    sdk="$(grep -E '^[[:space:]]*sdk\.dir[[:space:]]*=' "$props" | head -1 | sed 's/^[^=]*=//' | tr -d '\r' | xargs)"
    if [[ -n "$sdk" ]]; then
      for cand in "${sdk//\\//}/platform-tools/adb.exe" "${sdk//\\//}/platform-tools/adb"; do
        [[ -f "$cand" ]] && { printf '%s' "$cand"; return; }
      done
    fi
  fi
  printf 'adb'
}

# 定位 Python（用于解析 JSON 清单）
find_python() {
  local c
  for c in python3 python py; do
    command -v "$c" >/dev/null 2>&1 && { printf '%s' "$c"; return; }
  done
  printf ''
}

ADB_BIN="$(find_adb)"
PY_BIN="$(find_python)"

die() { printf '\033[31m错误：%s\033[0m\n' "$*" >&2; exit 1; }

require_device() {
  "$ADB_BIN" get-state >/dev/null 2>&1 || die "没有检测到已连接的设备（先跑 adb devices 看看）"
}

# ---------------------------------------------------------------------------
# 拉取函数清单，输出「functionId <TAB> 说明」逐行文本
# ---------------------------------------------------------------------------
fetch_pairs() {
  local raw
  raw="$("$ADB_BIN" shell cmd app_function list-app-functions --package "$PKG" 2>/dev/null | tr -d '\r')"
  [[ -n "$PY_BIN" ]] || die "需要 Python 来解析清单（找不到 python3/python/py）"
  printf '%s' "$raw" | "$PY_BIN" -c '
import json, sys
raw = sys.stdin.read().strip()
if not raw or raw == "{}":
    sys.exit(0)
try:
    data = json.loads(raw)
except Exception:
    sys.exit(0)
for pkg, items in data.items():
    for item in items:
        for key, val in item.items():
            if not key.startswith("AppFunctionStaticMetadata"):
                continue
            fid = (val.get("functionId") or [""])[0]
            desc = (val.get("description") or [""])[0].split("\n")[0].strip()
            print(f"{fid}\t{desc}")
'
}

cmd_list() {
  require_device
  local pairs
  pairs="$(fetch_pairs)"
  if [[ -z "$pairs" ]]; then
    cat <<'EOF'
系统没有索引到任何函数。

按顺序排查：
  1. AndroidManifest.xml 的 service 节点里是否有
       <property android:name="android.app.appfunctions.v2"
                 android:value="<appFunctionXmlFileName>.xml" />
     值必须正好等于注解里的 appFunctionXmlFileName 加 .xml
     —— 写错不会报任何错，只会静默索引到 0 个函数。
  2. APK 的 assets/ 下是否有那个 XML：
       unzip -l app/build/outputs/apk/debug/app-debug.apk | grep app_functions
  3. 手机上装的是不是最新构建的包
EOF
    exit 1
  fi

  printf '包名：%s\n\n' "$PKG"
  printf '%-20s  %s\n' "函数" "说明"
  printf '%-20s  %s\n' "--------------------" "----------------------------"
  while IFS=$'\t' read -r fid desc; do
    [[ -z "$fid" ]] && continue
    printf '%-20s  %s\n' "${fid##*#}" "$desc"
  done <<< "$pairs"
}

# 把简写解析成完整 functionId
resolve_fid() {
  local want="$1" fid out=""
  while IFS=$'\t' read -r fid _; do
    [[ -z "$fid" ]] && continue
    if [[ "${fid##*#}" == "$want" || "$fid" == "$want" ]]; then
      out="$fid"; break
    fi
  done <<< "$(fetch_pairs)"
  if [[ -z "$out" ]]; then
    # 退一步做模糊匹配
    while IFS=$'\t' read -r fid _; do
      [[ -z "$fid" ]] && continue
      [[ "$fid" == *"$want"* ]] && { out="$fid"; break; }
    done <<< "$(fetch_pairs)"
  fi
  [[ -n "$out" ]] || die "找不到函数「$want」，先跑：tools/appfunction.sh list"
  printf '%s' "$out"
}

cmd_describe() {
  local name="${1:-}"
  [[ -n "$name" ]] || die "用法：tools/appfunction.sh describe <函数名>"
  require_device
  local fid; fid="$(resolve_fid "$name")"
  printf 'functionId：%s\n\n' "$fid"
  echo "--- 系统索引到的元数据 ---"
  "$ADB_BIN" shell cmd app_function list-app-functions --package "$PKG" 2>/dev/null \
    | tr -d '\r' | awk -v id="$fid" 'index($0, id) { hit=1 } hit' | head -45
}

cmd_call() {
  local name="${1:-}" params="${2:-{\}}"
  [[ -n "$name" ]] || die "用法：tools/appfunction.sh call <函数名> [JSON 参数]"
  require_device
  local fid; fid="$(resolve_fid "$name")"
  local short="${fid##*#}"

  case "$short" in
    unlockDevice|runPointsTask)
      printf '\033[33m注意：%s 会产生真实订单或真实操作，结果不可撤销。\033[0m\n' "$short"
      read -r -p "确认执行？(yes/no) " ans
      [[ "$ans" == "yes" ]] || { echo "已取消"; exit 0; }
      ;;
  esac

  printf '调用 %s\n参数 %s\n\n' "$fid" "$params"
  "$ADB_BIN" shell "cmd app_function execute-app-function --package $PKG --function '$fid' --parameters '$params' --timeout-duration $TIMEOUT" 2>&1 | tr -d '\r'
}

# ---------------------------------------------------------------------------
case "${1:-}" in
  list|ls)       shift; cmd_list "$@" ;;
  describe|desc) shift; cmd_describe "$@" ;;
  call|exec)     shift; cmd_call "$@" ;;
  ""|-h|--help|help)
    sed -n '2,30p' "${BASH_SOURCE[0]}" | sed 's/^#\s\{0,1\}//'
    ;;
  *) die "未知命令「$1」，可用：list / describe / call" ;;
esac
