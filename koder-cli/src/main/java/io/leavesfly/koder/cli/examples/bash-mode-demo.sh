#!/bin/bash

# Koder Bash模式交互演示脚本
# 演示如何在对话模式下使用!前缀运行shell命令

echo "======================================================================"
echo "        Koder Bash模式交互演示"
echo "======================================================================"
echo ""
echo "此脚本模拟在Koder REPL中使用Bash模式的真实场景"
echo ""

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 模拟REPL提示符
function prompt() {
    echo -e "${BLUE}koder> ${NC}$1"
}

# 模拟命令输出
function output() {
    echo -e "${GREEN}$1${NC}"
}

# 模拟错误输出
function error() {
    echo -e "${RED}$1${NC}"
}

# 模拟提示信息
function info() {
    echo -e "${YELLOW}$1${NC}"
}

echo "【场景1: 文件浏览】"
echo "----------------------------------------------------------------------"
sleep 1

prompt "!ls -la"
sleep 0.5
ls -la
echo ""
sleep 1

prompt "!pwd"
sleep 0.5
pwd
echo ""
sleep 1

echo "【场景2: 文件操作】"
echo "----------------------------------------------------------------------"
sleep 1

prompt "!cat README.md | head -10"
sleep 0.5
if [ -f "README.md" ]; then
    cat README.md | head -10
else
    output "README.md: 文件不存在"
fi
echo ""
sleep 1

echo "【场景3: Git操作】"
echo "----------------------------------------------------------------------"
sleep 1

prompt "!git status"
sleep 0.5
if git rev-parse --git-dir > /dev/null 2>&1; then
    git status
else
    output "当前目录不是Git仓库"
fi
echo ""
sleep 1

prompt "!git log --oneline -5"
sleep 0.5
if git rev-parse --git-dir > /dev/null 2>&1; then
    git log --oneline -5 2>/dev/null || output "没有提交历史"
else
    output "当前目录不是Git仓库"
fi
echo ""
sleep 1

echo "【场景4: 系统信息查询】"
echo "----------------------------------------------------------------------"
sleep 1

prompt "!uname -a"
sleep 0.5
uname -a
echo ""
sleep 1

prompt "!java -version"
sleep 0.5
java -version 2>&1
echo ""
sleep 1

echo "【场景5: 文件搜索】"
echo "----------------------------------------------------------------------"
sleep 1

prompt "!find . -name '*.java' | head -5"
sleep 0.5
find . -name '*.java' 2>/dev/null | head -5 || output "未找到Java文件"
echo ""
sleep 1

echo "【场景6: 进程查看】"
echo "----------------------------------------------------------------------"
sleep 1

prompt "!ps aux | grep java | head -3"
sleep 0.5
ps aux | grep java | head -3 | grep -v grep || output "未找到Java进程"
echo ""
sleep 1

echo "【场景7: 安全限制演示】"
echo "----------------------------------------------------------------------"
sleep 1

prompt "!rm -rf /"
sleep 0.5
error "命令 'rm' 因安全原因被禁止执行"
echo ""
sleep 1

prompt "!shutdown now"
sleep 0.5
error "命令 'shutdown' 因安全原因被禁止执行"
echo ""
sleep 1

echo "【场景8: 与AI助手配合使用】"
echo "----------------------------------------------------------------------"
sleep 1

info "1. 先使用!命令查看项目结构"
prompt "!tree -L 2 -d"
sleep 0.5
if command -v tree &> /dev/null; then
    tree -L 2 -d 2>/dev/null | head -20
else
    find . -maxdepth 2 -type d 2>/dev/null | head -20
fi
echo ""
sleep 1

info "2. 然后向AI助手提问"
prompt "请分析这个项目的模块结构，并给出改进建议"
sleep 0.5
output "[AI助手回复] 正在分析项目结构..."
echo ""
sleep 1

echo "======================================================================"
echo "                         演示完成"
echo "======================================================================"
echo ""
echo "总结:"
echo "  ✅ Bash模式可以直接在对话中运行shell命令"
echo "  ✅ 使用!前缀即可，无需额外切换模式"
echo "  ✅ 支持管道、重定向等shell特性"
echo "  ✅ 内置安全限制，防止危险操作"
echo "  ✅ 可与AI助手无缝配合使用"
echo ""
echo "开始使用:"
echo "  cd Koder && mvn clean package"
echo "  java -jar koder-cli/target/koder.jar"
echo ""
