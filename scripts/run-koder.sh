#!/bin/bash

# Koder快速启动脚本（使用Spring Boot Maven插件）

# 设置颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}     Koder CLI - 快速启动         ${NC}"
echo -e "${GREEN}=====================================${NC}"
echo

# 设置JAVA_HOME
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo -e "${YELLOW}→ 使用Spring Boot运行Koder...${NC}"
echo

# 使用Spring Boot Maven插件运行
mvn spring-boot:run -pl koder-cli -DskipTests
