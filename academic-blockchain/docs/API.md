# 后端 REST API 速查

> Base URL：`http://127.0.0.1:8080/api`  
> 认证方式：`Authorization: Bearer <jwt>`（由 `/users/login` 返回）  
> 响应统一格式：`{ "code": 0, "message": "ok", "data": ... }`，`code != 0` 表示业务异常。

## 1. 用户

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/users/login` | 公开 | 钱包登录（首次自动注册）。Body: `{ walletAddress, username? }` |
| GET  | `/users/me` | 是 | 获取当前用户信息 |
| PUT  | `/users/me` | 是 | 更新昵称/电话/邮箱。Body: `{ username?, phone?, email? }` |
| GET  | `/users/by-wallet/{wallet}` | 是 | 按钱包地址查询用户 |

## 2. 成果

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/achievements` | 创建成果。Body: `{ name, summary?, category? }` |
| PUT  | `/achievements/{id}` | 更新成果（仅 `CREATED` 状态可改） |
| DELETE | `/achievements/{id}` | 删除成果（仅 `CREATED` 状态可删） |
| GET  | `/achievements/{id}` | 成果详情（含所有者、文件、链上记录、授权数） |
| GET  | `/achievements/mine` | 当前用户的所有成果 |
| GET  | `/achievements?pageNum=1&pageSize=10&keyword=xxx` | 分页搜索 |

## 3. 文件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/files/upload` | 上传文件（multipart）。表单字段：`achievementId`、`file` |
| GET  | `/files/{fileId}/meta` | 文件元数据 |
| GET  | `/files/{fileId}/download` | 下载文件（需是所有者或被授权方） |

## 4. 链上存证

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/records/register` | 把指定文件上链。Body: `{ achievementId, fileId }` |
| GET  | `/records/by-achievement/{achievementId}` | 查找成果的存证记录 |
| GET  | `/records/by-hash/{fileHash}` | 按哈希查找存证记录 |

## 5. 核验

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/verify` | 公开 | 上传文件，链上核验 |
| GET  | `/verify/by-hash?fileHash=0x...` | 公开 | 按哈希核验 |
| GET  | `/verify/logs?mine=true` | 是 | 核验日志（mine=true 仅看自己） |

## 6. 授权

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/authorizations/grant` | 创建授权。Body: `{ achievementId, granteeAddress, permissionType, expireTime? }` |
| POST | `/authorizations/{authorizationId}/revoke` | 撤销授权 |
| GET  | `/authorizations/granted` | 我发出的授权 |
| GET  | `/authorizations/received?onlyActive=true` | 我收到的授权 |
| GET  | `/authorizations/by-achievement/{achievementId}` | 某成果的所有授权 |
| GET  | `/authorizations/check?achievementId=1&wallet=0x...` | 链上权限校验 |

## 7. 后台管理（需 ADMIN 角色）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/stats` | 统计概览 |
| GET | `/admin/users` | 用户列表 |
| GET | `/admin/achievements?keyword=` | 成果列表 |
| GET | `/admin/authorizations` | 授权记录 |
| GET | `/admin/verify-logs` | 核验日志 |

## 8. 健康检查

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/health` | 返回链上节点 / 合约地址等运行时信息 |

## 错误码

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 401 | 未登录 / 令牌无效 |
| 403 | 权限不足 |
| 4000 | 业务异常（具体看 message） |
| 4001 | 参数校验失败 |
| 5000 | 服务器内部错误 |
