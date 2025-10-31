#!/bin/bash

# Koder系统级安装脚本
# 将koder.jar安装到系统路径，可以在任何地方使用

# 设置颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}     Koder - 系统级安装            ${NC}"
echo -e "${GREEN}=====================================${NC}"
echo

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

# 安装目录
INSTALL_DIR="$HOME/.koder"
BIN_DIR="$HOME/bin"

# 创建目录
mkdir -p "$INSTALL_DIR"
mkdir -p "$BIN_DIR"

echo -e "${YELLOW}→ 复制koder.jar到 $INSTALL_DIR${NC}"
cp "$JAR_FILE" "$INSTALL_DIR/koder.jar"

echo -e "${YELLOW}→ 创建启动脚本到 $BIN_DIR/koder${NC}"

# 创建启动脚本
cat > "$BIN_DIR/koder" << 'EOF'
#!/bin/bash

# Koder启动脚本

# 自动检测JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    # macOS
    if [ -f "/usr/libexec/java_home" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
    fi
    
    # Linux - 尝试常见路径
    if [ -z "$JAVA_HOME" ]; then
        for java_dir in /usr/lib/jvm/java-17-* /usr/java/jdk-17*; do
            if [ -d "$java_dir" ]; then
                export JAVA_HOME="$java_dir"
                break
            fi
        done
    fi
fi

# 检查Java
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "错误: 未找到Java 17"
    echo "请设置JAVA_HOME环境变量"
    exit 1
fi

# 运行koder
exec "$JAVA_HOME/bin/java" -jar "$HOME/.koder/koder.jar" "$@"
EOF

chmod +x "$BIN_DIR/koder"

echo
echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}  ✓ 安装成功！${NC}"
echo -e "${GREEN}=====================================${NC}"
echo
echo -e "${YELLOW}安装位置:${NC}"
echo -e "  JAR文件: ${GREEN}$INSTALL_DIR/koder.jar${NC}"
echo -e "  启动脚本: ${GREEN}$BIN_DIR/koder${NC}"
echo
echo -e "${YELLOW}使用方式:${NC}"
echo -e "  在任何目录下直接运行: ${GREEN}koder${NC}"
echo
echo -e "${YELLOW}注意:${NC}"
echo -e "  请确保 ${GREEN}$BIN_DIR${NC} 在你的PATH中"
echo -e "  可在 ~/.bashrc 或 ~/.zshrc 中添加:"
echo -e "    ${GREEN}export PATH=\"\$HOME/bin:\$PATH\"${NC}"
echo
