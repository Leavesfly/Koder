#!/bin/bash

# Koder打包脚本 - 生成可执行JAR

# 设置颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}     Koder - 打包可执行JAR         ${NC}"
echo -e "${GREEN}=====================================${NC}"
echo

# 设置JAVA_HOME
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo -e "${YELLOW}→ 清理旧的构建文件...${NC}"
mvn clean

echo
echo -e "${YELLOW}→ 编译并打包...${NC}"
mvn package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}  ✗ 打包失败${NC}"
    exit 1
fi

echo
echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}  ✓ 打包成功！${NC}"
echo -e "${GREEN}=====================================${NC}"
echo
echo -e "${YELLOW}可执行JAR位置:${NC}"
echo -e "  ${GREEN}koder-cli/target/koder.jar${NC}"
echo
echo -e "${YELLOW}运行方式:${NC}"
echo -e "  ${GREEN}java -jar koder-cli/target/koder.jar${NC}"
echo
echo -e "${YELLOW}或使用快捷脚本:${NC}"
echo -e "  ${GREEN}./run-jar.sh${NC}"
echo
