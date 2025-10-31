#!/bin/bash

# Koder启动脚本
# 使用Java 17运行Koder CLI

# 设置颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}     Koder CLI - Java版启动脚本     ${NC}"
echo -e "${GREEN}=====================================${NC}"
echo

# 检查Java版本
echo -e "${YELLOW}→ 检查Java环境...${NC}"
if [ -z "$JAVA_HOME" ]; then
    # 尝试自动设置JAVA_HOME
    if [ -f "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin/java" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"
        echo -e "${GREEN}  ✓ 已自动设置JAVA_HOME: $JAVA_HOME${NC}"
    else
        echo -e "${RED}  ✗ 错误: JAVA_HOME未设置且未找到JDK 17${NC}"
        echo -e "${YELLOW}  请手动设置JAVA_HOME环境变量${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}  ✓ JAVA_HOME: $JAVA_HOME${NC}"
fi

# 检查Java版本
JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo -e "${GREEN}  ✓ Java版本: $JAVA_VERSION${NC}"
echo

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR"

# 切换到项目根目录
cd "$PROJECT_ROOT"

# 检查是否需要编译
if [ ! -d "koder-cli/target/classes" ] || [ "$1" == "--rebuild" ]; then
    echo -e "${YELLOW}→ 编译项目...${NC}"
    mvn clean compile -DskipTests
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}  ✗ 编译失败${NC}"
        exit 1
    fi
    echo -e "${GREEN}  ✓ 编译成功${NC}"
    echo
fi

# 运行应用
echo -e "${YELLOW}→ 启动Koder CLI...${NC}"
echo

# 设置classpath
CLASSPATH="koder-cli/target/classes"
CLASSPATH="$CLASSPATH:koder-core/target/classes"
CLASSPATH="$CLASSPATH:koder-models/target/classes"
CLASSPATH="$CLASSPATH:koder-tools/target/classes"
CLASSPATH="$CLASSPATH:koder-mcp/target/classes"
CLASSPATH="$CLASSPATH:koder-agent/target/classes"

# 添加所有依赖jar
for jar in ~/.m2/repository/**/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

# 运行Spring Boot应用
"$JAVA_HOME/bin/java" \
    -cp "$CLASSPATH" \
    -Dspring.profiles.active=dev \
    -Dlogging.level.io.leavesfly.koder=DEBUG \
    io.leavesfly.koder.cli.KoderCliApplication "$@"
