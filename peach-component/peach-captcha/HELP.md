# Peach Captcha 验证码组件

## 简介 / Introduction
Peach Captcha 是一款轻量级、高扩展性的 Java 验证码组件，旨在为 Web 应用提供安全、易用的行为验证码服务。项目采用 JDK 1.8+ 开发，支持多种主流验证码类型，内置缓存支持（Redis/Local），并提供灵活的配置选项。

## 核心功能 / Features

本项目目前支持以下四种核心验证码类型：

### 1. 滑动拼图验证码 (Block Puzzle Captcha)
用户需要拖动滑块将拼图块对准缺口。
- **特点**：交互自然，用户体验好，是目前最主流的验证码形式。
- **安全性**：通过校验坐标偏差、滑动轨迹（可选扩展）等方式防止机器爆破。

### 2. 点选文字验证码 (Click Word Captcha)
用户需要按照提示顺序点击图片中的汉字。
- **特点**：安全性极高，难以被 OCR 破解。
- **安全性**：校验点击坐标的顺序和位置偏差。

### 3. 旋转拼图验证码 (Rotate Puzzle Captcha)
用户需要拖动滑块将旋转的图片还原到正向（0度）。
- **特点**：趣味性强，无需背景图缺口，只需一张图即可生成。
- **安全性**：校验旋转角度的偏差值（允许一定误差范围）。

### 4. 文字/算术验证码 (Text/Arithmetic Captcha)
用户输入图片显示的字符或计算结果。
- **特点**：传统、简单、兼容性好。
- **安全性**：包含干扰线和噪点，支持动态字体大小。

## 优劣势分析 / Pros & Cons

| 验证码类型 | 优势 (Pros) | 劣势 (Cons) | 适用场景 |
| :--- | :--- | :--- | :--- |
| **滑动拼图** | 用户体验极佳，交互简单 | 需要图库支持，高级 AI 可能识别缺口 | 登录、注册、一般风控 |
| **点选文字** | 安全性高，OCR 识别难度大 | 用户交互成本稍高（需点击多次） | 高安全级操作（支付、改密） |
| **旋转拼图** | 新颖有趣，无需复杂图库处理 | 图片若无方向性（如抽象图）则难以识别 | 创意交互、移动端 |
| **文字/算术** | 实现简单，无客户端依赖 | 用户体验一般（需键盘输入），易被 OCR 破解 | 兜底方案、老旧系统 |

## 实现原理 / Implementation Principles

### 1. 滑动拼图 (Block Puzzle)
1.  **初始化**：随机选择一张背景图。
2.  **抠图**：根据预设模板（拼图形状）在背景图随机位置抠出一块图片。
3.  **生成**：
    -   `originalImage`：带有缺口的背景图（Base64）。
    -   `slidingImage`：被抠出的拼图块（Base64）。
    -   `token`：唯一标识。
4.  **缓存**：将抠图的 `X, Y` 坐标存入 Redis，Key 为 `token`。
5.  **校验**：前端提交用户拖动的 `X` 坐标，后端比对缓存中的 `X` 坐标，允许误差（如 ±5px）。

### 2. 点选文字 (Click Word)
1.  **生成**：
    -   随机选取 4-5 个汉字，随机颜色、角度绘制在背景图上。
    -   随机选择其中 3 个字作为“答案”，并在顶部生成提示栏图片。
2.  **缓存**：将所有文字的中心坐标（PointVO）序列化后存入 Redis。
3.  **校验**：
    -   前端提交用户点击的坐标序列（经过 AES 加密）。
    -   后端解密后，依次计算用户点击坐标与缓存真实坐标的欧氏距离。
    -   若所有点的距离均在阈值（如 25px）内，则通过。

### 3. 旋转拼图 (Rotate Puzzle)
1.  **生成**：
    -   选取一张图片，随机生成旋转角度（0-360度）。
    -   使用 `AffineTransform` 对图片进行旋转处理。
    -   **关键点**：同时返回旋转后的图片 Base64 和 **原图 Base64**（用于前端展示目标或参考）。
2.  **缓存**：计算还原需要的角度（360 - 随机角度），存入 Redis。
3.  **校验**：前端提交用户旋转的角度，后端校验与缓存角度的差值，允许误差（如 ±5度）。

### 4. 文字/算术 (Text/Arithmetic)
1.  **生成**：
    -   随机决定生成算术题（1+1=?）还是随机字符（ABCD）。
    -   **动态排版**：计算文字宽度，若超出图片宽度则自动缩小字体，若仍超出则重试生成，确保文字不被截断。
    -   绘制干扰线和噪点。
2.  **缓存**：将正确答案（计算结果或字符）存入 Redis。
3.  **校验**：比对用户输入的字符串与缓存答案（忽略大小写）。

## 快速开始 / Quick Start

### 引入依赖
```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-captcha-autoconfigure</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 配置 (application.properties)
```properties
# 验证码类型: blockPuzzle, clickWord, text, rotate
peach.captcha.type=blockPuzzle
# 缓存类型: local, redis
peach.captcha.cache-type=redis
```

### 接口调用
1.  **获取验证码**: `POST /captcha/get`
2.  **校验验证码**: `POST /captcha/check`
3.  **二次校验验证码**: `POST /captcha/verification`

## 目录结构
- `com.peach.captcha.service.impl`: 核心实现类
    - `BlockPuzzleCaptchaServiceImpl`: 滑动拼图
    - `ClickWordCaptchServiceImpl`: 点选文字
    - `RotatePuzzleCaptchaServiceImpl`: 旋转拼图
    - `TextCaptchaServiceImpl`: 文字/算术

---
**Author**: Mr Shu
**License**: Apache 2.0
