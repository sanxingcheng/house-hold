# Bug Fix Record — house-hold 后端代码检视

**检视范围：** `house-hold-auth-user` 服务 + `house-hold-gateway` 服务  
**检视日期：** 2026-03-01  
**修复数量：** 8 项（Critical × 1，High × 2，Medium × 2，Low × 3）

---

## BUG-001 [Critical] UserMapper.xml — `updateProfile` 生成 `SET SET` 双关键字，`updated_at` 置于 SET 子句外

**文件：** `house-hold-auth-user/src/main/resources/mapper/UserMapper.xml`

**问题描述：**  
MyBatis `<set>` 标签会自动拼接 `SET` 关键字；原代码在 `UPDATE user_base` 后手动写了 `SET`，再套 `<set>` 标签，导致最终 SQL 为：
```sql
UPDATE user_base SET SET name = ?, ...
```
同时 `updated_at = CURRENT_TIMESTAMP` 写在 `</set>` 之后、`WHERE` 之前，既不在 SET 子句内又无逗号分隔，构成语法错误，执行时必然抛 `SQLSyntaxErrorException`。

**修复方案：**  
- 移除 SQL 语句中手写的 `SET` 关键字，由 `<set>` 标签唯一生成。  
- 将 `updated_at = CURRENT_TIMESTAMP,` 移入 `<set>` 块内（`<set>` 会自动去除末尾逗号）。

**修改前：**
```xml
<update id="updateProfile">
    UPDATE user_base SET
    <set>
        <if test="name != null">name = #{name},</if>
        ...
    </set>
    updated_at = CURRENT_TIMESTAMP
    WHERE id = #{id}
</update>
```

**修改后：**
```xml
<update id="updateProfile">
    UPDATE user_base
    <set>
        <if test="name != null">name = #{name},</if>
        ...
        updated_at = CURRENT_TIMESTAMP,
    </set>
    WHERE id = #{id}
</update>
```

---

## BUG-002 [High] FamilyService.join — `Long.parseLong` 未捕获，非法 familyId 导致 500

**文件：** `house-hold-auth-user/.../service/FamilyService.java`

**问题描述：**  
`join()` 方法中直接调用 `Long.parseLong(req.getFamilyId().trim())`，当用户传入非数字字符串（如 `"abc"`、`"null"` 等）时抛出 `NumberFormatException`。该异常未在 `GlobalExceptionHandler` 中注册，由 Spring 默认转为 HTTP 500。

**修复方案：**  
用 try-catch 捕获 `NumberFormatException`，转抛已有的 `BadRequestException` 使其返回 HTTP 400。

**修改后：**
```java
Long familyId;
try {
    familyId = Long.parseLong(req.getFamilyId().trim());
} catch (NumberFormatException e) {
    throw new BadRequestException("家庭ID格式不正确");
}
```

---

## BUG-003 [High] Gateway — OPTIONS 预检请求被强制 JWT 验证，CORS 跨域全面失效

**文件：** `house-hold-gateway/.../filter/JwtAuthGlobalFilter.java`

**问题描述：**  
浏览器在发送跨域请求前会先发出 HTTP OPTIONS 预检请求（无 `Authorization` 头）。原 Filter 对 `/user/**`、`/family/**` 路径统一要求 JWT，导致所有 OPTIONS 请求返回 401，前端跨域接口完全不可用。

**修复方案：**  
在路径检查之前，率先判断请求方法是否为 `OPTIONS`，是则直接放行。

**修改后：**
```java
// Allow CORS preflight to pass through without JWT validation
if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
    return chain.filter(exchange);
}
```

---

## BUG-004 [Medium] Gateway — 公开路径（login/register）未剥离客户端伪造的 `X-User-Id`/`X-Family-Id`

**文件：** `house-hold-gateway/.../filter/JwtAuthGlobalFilter.java`

**问题描述：**  
对于 `SKIP_PATHS`（`/auth/login`、`/auth/register`）直接放行，客户端可在请求头中携带 `X-User-Id: 999` 转发至下游服务。虽然当前登录/注册端点未读取该头，但若后续端点疏于防范则存在身份伪造风险。

**修复方案：**  
在 Filter 入口处、所有分支判断之前，无条件剥离 `X-User-Id` 与 `X-Family-Id` 请求头，确保这两个头仅由网关在 JWT 校验通过后注入。

**修改后：**
```java
// Strip client-supplied trusted headers to prevent injection
ServerHttpRequest request = exchange.getRequest().mutate()
        .headers(h -> { h.remove("X-User-Id"); h.remove("X-Family-Id"); })
        .build();
exchange = exchange.mutate().request(request).build();
```

---

## BUG-005 [Medium] 生日字段缺乏格式校验，非法格式导致 500

**文件：**  
- `house-hold-auth-user/.../dto/request/UserProfileUpdateRequest.java`  
- `house-hold-auth-user/.../dto/request/RegisterRequest.java`  
- `house-hold-auth-user/.../exception/GlobalExceptionHandler.java`

**问题描述：**  
`AuthService.register` 和 `UserService.updateProfile` 均直接调用 `LocalDate.parse(birthday)` 而未做格式预校验。当用户传入 `"2024/01/01"`、`"今天"` 等非 ISO 格式时，抛出 `DateTimeParseException`（未检查异常），未被任何 `@ExceptionHandler` 捕获，框架默认返回 HTTP 500。

**修复方案：**  
1. 在两个 DTO 的 `birthday` 字段上添加 `@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")` 注解，在 Controller 校验阶段（`@Valid`）即拦截非法格式，返回 400。  
2. 在 `GlobalExceptionHandler` 补充 `DateTimeParseException` 处理器作为兜底，防止校验绕过时仍返回 500。

**修改后（DTO 示例）：**
```java
@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "生日格式应为 YYYY-MM-DD")
private String birthday;
```

**修改后（兜底 Handler）：**
```java
@ExceptionHandler(DateTimeParseException.class)
public ResponseEntity<Map<String, String>> handleDateTimeParse(DateTimeParseException e) {
    return ResponseEntity.badRequest().body(errorBody("VALIDATION_ERROR", "日期格式不正确，应为 YYYY-MM-DD"));
}
```

---

## BUG-006 [Low] FamilyService.create — 创建家庭响应中 `joinedAt` 为 null

**文件：** `house-hold-auth-user/.../service/FamilyService.java`

**问题描述：**  
`create()` 方法在内存中构造 `FamilyMemberRole` 对象后直接传入 `toResponse()`，而 `joinedAt` 字段未赋值（依赖数据库 `DEFAULT CURRENT_TIMESTAMP`）。由于未从数据库回读，响应的 `joinedAt` 字段始终为 `null`，与实际存储值不符。

**修复方案：**  
在 `familyMemberRoleMapper.insert(role)` 调用前，手动设置 `role.setJoinedAt(LocalDateTime.now())` 以使内存对象与数据库值保持一致。

**修改后：**
```java
role.setJoinedAt(LocalDateTime.now());
familyMemberRoleMapper.insert(role);
```

---

## BUG-007 [Low] Cache — `loadLocks.remove(fullKey)` 非原子操作，竞态下可能双重加载 DB

**文件：**  
- `house-hold-auth-user/.../cache/UserCacheService.java`  
- `house-hold-auth-user/.../cache/FamilyCacheService.java`

**问题描述：**  
Single-flight 逻辑在 `finally` 块中先 `lock.unlock()` 再 `loadLocks.remove(fullKey)`，两步非原子。若线程 A 释放锁后、删除 key 前，线程 B 恰好 `computeIfAbsent` 拿到同一锁对象进入临界区，随后线程 A 删除该 key，线程 C 又创建新锁，导致 B、C 并发加载数据库，破坏 single-flight 语义。

**修复方案：**  
使用 `ConcurrentHashMap.remove(key, value)` 的原子版本，仅当 map 中的值与当前锁对象相同时才删除，避免误删其他线程新注册的锁。

**修改后：**
```java
} finally {
    lock.unlock();
    loadLocks.remove(fullKey, lock); // atomic: only remove if still this exact lock
}
```

---

## BUG-008 [Low] UserCacheService — 注入了 `UserMapper` 但从未使用（死代码）

**文件：** `house-hold-auth-user/.../cache/UserCacheService.java`

**问题描述：**  
构造函数参数中包含 `UserMapper userMapper` 并赋值给字段，但整个类内无任何调用点。Cache 的数据加载通过外部传入的 `Supplier<UserProfileResponse> loader` 回调完成，`userMapper` 字段为残留死代码，增加不必要的依赖耦合。

**修复方案：**  
移除 `UserMapper` 字段声明、import 及构造函数参数。

---

## 修复汇总

| ID      | 严重等级  | 影响范围              | 状态   |
|---------|-----------|-----------------------|--------|
| BUG-001 | Critical  | 所有 updateProfile 调用 | ✅ 已修复 |
| BUG-002 | High      | 加入家庭接口           | ✅ 已修复 |
| BUG-003 | High      | 所有跨域前端请求        | ✅ 已修复 |
| BUG-004 | Medium    | 登录/注册路径安全性     | ✅ 已修复 |
| BUG-005 | Medium    | 注册/更新生日字段       | ✅ 已修复 |
| BUG-006 | Low       | 创建家庭响应数据        | ✅ 已修复 |
| BUG-007 | Low       | 缓存并发一致性          | ✅ 已修复 |
| BUG-008 | Low       | 代码整洁度              | ✅ 已修复 |
