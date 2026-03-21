#!/usr/bin/env bash
# Peach 后端代码风格校验脚本
# 用法: bash check-style.sh <file_or_directory>
# 返回: 0=通过, 1=存在违规

set -euo pipefail

TARGET="${1:-.}"
ERRORS=0
WARNINGS=0

RED='\033[0;31m'
YELLOW='\033[0;33m'
GREEN='\033[0;32m'
NC='\033[0m'

error() { echo -e "${RED}[ERROR]${NC} $1"; ((ERRORS++)); }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1";  ((WARNINGS++)); }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }

echo "========================================="
echo " Peach Backend Code Style Checker"
echo "========================================="
echo ""

# ─── 1. Java 9+ 语法检查 ───
echo ">> [1/9] 检查 Java 9+ 禁止语法..."
while IFS= read -r f; do
    # var 关键字 (排除注释和字符串)
    if grep -Pn '^\s+var\s+\w+\s*=' "$f" 2>/dev/null; then
        error "$f: 使用了 var 关键字 (Java 10+, 禁止)"
    fi
    # List.of / Map.of / Set.of
    if grep -Pn '\b(List|Map|Set)\.(of|copyOf)\(' "$f" 2>/dev/null; then
        error "$f: 使用了 List.of/Map.of/Set.of (Java 9+, 禁止)"
    fi
    # 文本块 """
    if grep -Pn '"""' "$f" 2>/dev/null; then
        error "$f: 使用了文本块 (Java 13+, 禁止)"
    fi
    # record 类
    if grep -Pn '^\s*public\s+record\s+' "$f" 2>/dev/null; then
        error "$f: 使用了 record (Java 14+, 禁止)"
    fi
done < <(find "$TARGET" -name "*.java" -type f 2>/dev/null)

# ─── 2. System.out 检查 ───
echo ">> [2/9] 检查 System.out.println..."
while IFS= read -r f; do
    if grep -Pn 'System\.(out|err)\.(print|println)' "$f" 2>/dev/null; then
        error "$f: 使用了 System.out/err (应使用 @Slf4j)"
    fi
done < <(find "$TARGET" -name "*.java" -type f 2>/dev/null)

# ─── 3. Controller 注解检查 ───
echo ">> [3/9] 检查 Controller 注解规范..."
while IFS= read -r f; do
    if grep -q '@RestController' "$f" 2>/dev/null; then
        if ! grep -q '@Tag' "$f" 2>/dev/null; then
            error "$f: Controller 缺少 @Tag 注解"
        fi
        if ! grep -q '@Slf4j' "$f" 2>/dev/null; then
            warn "$f: Controller 缺少 @Slf4j"
        fi
        if ! grep -q '@Indexed' "$f" 2>/dev/null; then
            warn "$f: Controller 缺少 @Indexed"
        fi
        # 检查方法是否有 @Operation
        method_count=$(grep -cP '(public|protected)\s+\w+.*\(.*\)\s*\{' "$f" 2>/dev/null || echo 0)
        operation_count=$(grep -c '@Operation' "$f" 2>/dev/null || echo 0)
        if [ "$method_count" -gt 0 ] && [ "$operation_count" -eq 0 ]; then
            error "$f: Controller 方法缺少 @Operation 注解"
        fi
        # 检查方法命名是否以 query/save/modify/delete 开头
        method_names=$(grep -oP '(public|protected)\s+(Response|\w+)\s+(query|save|modify|delete)\w*\s*\(' "$f" 2>/dev/null || true)
        all_methods=$(grep -oP '(public|protected)\s+(Response|\w+)\s+\w+\s*\(' "$f" 2>/dev/null || true)
        if [ -n "$all_methods" ]; then
            # 检查非查询方法是否缺少 @UserOperLog
            non_query_methods=$(grep -oP '(public|protected)\s+(Response|\w+)\s+(save|modify|delete)\w*\s*\(' "$f" 2>/dev/null || true)
            if [ -n "$non_query_methods" ]; then
                if ! grep -q '@UserOperLog' "$f" 2>/dev/null; then
                    error "$f: 非 query 方法缺少 @UserOperLog 注解"
                fi
            fi
        fi
    fi
done < <(find "$TARGET" -name "*Controller.java" -type f 2>/dev/null)

# ─── 4. Service 注解检查 ───
echo ">> [4/9] 检查 Service 注解规范..."
while IFS= read -r f; do
    if grep -q '@Service' "$f" 2>/dev/null; then
        if ! grep -q '@Slf4j' "$f" 2>/dev/null; then
            warn "$f: ServiceImpl 缺少 @Slf4j"
        fi
        if ! grep -q '@Indexed' "$f" 2>/dev/null; then
            warn "$f: ServiceImpl 缺少 @Indexed"
        fi
    fi
done < <(find "$TARGET" -name "*ServiceImpl.java" -type f 2>/dev/null)

# ─── 5. DAO 注解检查 ───
echo ">> [5/9] 检查 DAO 注解规范..."
while IFS= read -r f; do
    if grep -q 'interface.*Dao' "$f" 2>/dev/null; then
        if ! grep -q '@MybatisDao' "$f" 2>/dev/null; then
            error "$f: DAO 缺少 @MybatisDao 注解"
        fi
        if ! grep -q 'PeachDao' "$f" 2>/dev/null; then
            warn "$f: DAO 未继承 PeachDao"
        fi
    fi
done < <(find "$TARGET" -name "*Dao.java" -type f 2>/dev/null)

# ─── 6. 命名规范检查 ───
echo ">> [6/9] 检查命名规范..."
while IFS= read -r f; do
    basename=$(basename "$f" .java)
    # Service 接口应以 I 开头
    if [[ "$f" == *"/service/"* ]] && [[ "$basename" != *"Impl"* ]] && [[ "$basename" != I* ]] && grep -q 'interface' "$f" 2>/dev/null; then
        warn "$f: Service 接口建议以 I 开头 (如 IXxxService)"
    fi
    # DO/DTO/QO/VO 命名检查
    if [[ "$f" == *"/entity/"* ]] && [[ "$basename" != *"DO" ]] && [[ "$basename" != "Base"* ]] && [[ "$basename" != "Peach"* ]]; then
        warn "$f: entity 包下的类建议以 DO 结尾"
    fi
done < <(find "$TARGET" -name "*.java" -type f 2>/dev/null)

# ─── 7. MyBatis XML 检查 ───
echo ">> [7/9] 检查 MyBatis XML 规范..."
while IFS= read -r f; do
    # SELECT * 检查
    if grep -Pin 'SELECT\s+\*\s+FROM' "$f" 2>/dev/null; then
        error "$f: MyBatis XML 中使用了 SELECT * (禁止)"
    fi
    # namespace 是否为空
    if grep -P 'namespace\s*=\s*""' "$f" 2>/dev/null; then
        error "$f: Mapper namespace 为空"
    fi
    # 是否缺少必要 SQL 片段
    for frag in allColumn allColumnAlias allColumnValue allColumnCond; do
        if ! grep -q "id=\"$frag\"" "$f" 2>/dev/null; then
            warn "$f: 缺少 SQL 片段 <sql id=\"$frag\">"
        fi
    done
done < <(find "$TARGET" -name "*.xml" -path "*/mapper/*" -type f 2>/dev/null)

# ─── 8. Serializable 检查 ───
echo ">> [8/9] 检查 Serializable..."
while IFS= read -r f; do
    basename_f=$(basename "$f" .java)
    if [[ "$basename_f" == *"DO" ]] || [[ "$basename_f" == *"DTO" ]] || [[ "$basename_f" == *"QO" ]] || [[ "$basename_f" == *"VO" ]]; then
        if ! grep -q 'Serializable' "$f" 2>/dev/null; then
            error "$f: 数据对象未实现 Serializable"
        fi
        if ! grep -q 'serialVersionUID' "$f" 2>/dev/null; then
            warn "$f: 数据对象缺少 serialVersionUID"
        fi
    fi
done < <(find "$TARGET" -name "*.java" -type f 2>/dev/null)

# ─── 9. Controller 方法命名和 @UserOperLog 检查 ───
echo ">> [9/10] 检查 Controller 方法命名和 @UserOperLog..."
while IFS= read -r f; do
    if grep -q '@RestController' "$f" 2>/dev/null; then
        # 检查方法命名是否以 query/save/modify/delete 开头
        # 获取所有 public 方法
        method_lines=$(grep -n 'public Response' "$f" 2>/dev/null || true)
        if [ -n "$method_lines" ]; then
            echo "$method_lines" | while IFS= read -r line; do
                method_name=$(echo "$line" | sed -n 's/.*public Response \([a-zA-Z]*\).*/\1/p' || true)
                if [ -n "$method_name" ]; then
                    if [[ ! "$method_name" =~ ^(query|save|modify|delete) ]]; then
                        warn "$f: Controller 方法 $method_name 应以 query/save/modify/delete 开头"
                    fi
                    # 如果不是 query 开头，检查是否有 @UserOperLog
                    if [[ ! "$method_name" =~ ^query ]] && [[ "$method_name" =~ ^(save|modify|delete) ]]; then
                        # 获取方法所在行号
                        line_num=$(echo "$line" | cut -d: -f1)
                        # 检查该方法上方是否有 @UserOperLog
                        before_lines=$(sed -n "1,$((line_num-1))p" "$f" 2>/dev/null || true)
                        if ! echo "$before_lines" | grep -q '@UserOperLog'; then
                            error "$f: Controller 方法 $method_name 缺少 @UserOperLog 注解"
                        fi
                    fi
                fi
            done
        fi
    fi
done < <(find "$TARGET" -name "*Controller.java" -type f 2>/dev/null)

# ─── 10. 参数校验规范检查 ───
echo ">> [10/11] 检查参数校验规范..."
# 检查 Controller 使用 @Validated 时，对应的 QO/DTO 应该有校验规则
while IFS= read -r f; do
    # 获取所有使用 @Validated 的方法
    if grep -q '@Validated' "$f" 2>/dev/null; then
        # 提取所有参数类型（QO/DTO）
        param_types=$(grep -oE '@Validated\([^)]+\)\s+@RequestBody\s+\w+\s+\w+' "$f" 2>/dev/null | grep -oE '\w+(?:QO|DTO)' || true)
        if [ -n "$param_types" ]; then
            for param_type in $param_types; do
                # 查找对应的 QO/DTO 文件
                qo_dto_file=$(find "$TARGET" -name "${param_type}.java" -type f 2>/dev/null | head -1)
                if [ -n "$qo_dto_file" ]; then
                    # 检查该文件是否有校验注解
                    if ! grep -qE '@NotNull|@NotBlank|@NotEmpty|@Size|@Pattern|@Min|@Max|@DecimalMin|@DecimalMax' "$qo_dto_file" 2>/dev/null; then
                        warn "$f: Controller 使用了 @Validated 但参数 $param_type 没有定义校验规则"
                    fi
                fi
            done
        fi
    fi
done < <(find "$TARGET" -name "*Controller.java" -type f 2>/dev/null)

# ─── 11. 主键类型检查 ───
echo ">> [11/11] 检查主键类型和时间字段规范..."
while IFS= read -r f; do
    basename_f=$(basename "$f" .java)
    # 只检查 DO 类
    if [[ "$basename_f" == *"DO" ]] && [[ "$basename_f" != "PeachDO" ]]; then
        # 检查主键是否使用自增
        if grep -qE '@GeneratedValue.*IDENTITY' "$f" 2>/dev/null; then
            error "$f: DO 主键禁止使用自增 (@GeneratedValue + IDENTITY)"
        fi
        # 检查主键是否为 Long/Integer 类型
        if grep -qE '@Id' "$f" 2>/dev/null; then
            # 查找 @Id 下的字段定义
            id_field=$(sed -n '/@Id/,/private/p' "$f" 2>/dev/null | grep -E 'private (Long|Integer)' || true)
            if [ -n "$id_field" ]; then
                error "$f: DO 主键必须使用 String 类型 (UUID)，禁止使用 Long/Integer"
            fi
        fi
        # 检查是否使用 Date/LocalDateTime 类型
        if grep -qE 'private (Date|LocalDateTime|Timestamp)' "$f" 2>/dev/null; then
            error "$f: 时间字段必须使用 String 类型，禁止使用 Date/LocalDateTime/Timestamp"
        fi
    fi
done < <(find "$TARGET" -name "*.java" -type f 2>/dev/null)

# ─── 结果汇总 ───
echo ""
echo "========================================="
echo " 检查完成"
echo "========================================="
echo -e " Errors:   ${RED}${ERRORS}${NC}"
echo -e " Warnings: ${YELLOW}${WARNINGS}${NC}"
echo ""

if [ "$ERRORS" -gt 0 ]; then
    echo -e "${RED}❌ 校验未通过，存在 $ERRORS 个错误必须修复${NC}"
    exit 1
else
    if [ "$WARNINGS" -gt 0 ]; then
        echo -e "${YELLOW}⚠️  校验通过，但存在 $WARNINGS 个警告建议修复${NC}"
    else
        echo -e "${GREEN}✅ 校验全部通过${NC}"
    fi
    exit 0
fi
