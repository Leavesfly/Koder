#!/bin/bash

# Koder JAR运行脚本

# 设置颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 设置JAVA_HOME
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

JAR_FILE="koder-cli/target/koder.jar"

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}错误: JAR文件不存在${NC}"
    echo -e "${YELLOW}请先运行: ./build-jar.sh${NC}"
    exit 1
fi

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}     Koder - JAR启动              ${NC}"
echo -e "${GREEN}=====================================${NC}"
echo

# 运行JAR
"$JAVA_HOME/bin/java" -jar "$JAR_FILE" "$@"
