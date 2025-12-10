<#ftl encoding="UTF-8">
<#assign currentYear = .now?string("yyyy")>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>生日祝福 - ${companyName!"我们公司"}</title>
    <style>
        body {
            font-family: 'Microsoft YaHei', 'Segoe UI', Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            margin: 0;
            padding: 20px;
            background-color: #f9f9f9;
        }
        .email-container {
            max-width: 600px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }
        .header {
            background: linear-gradient(135deg, ${primaryColor!"#667eea"} 0%, ${secondaryColor!"#764ba2"} 100%);
            color: white;
            padding: 40px 20px;
            text-align: center;
        }
        .birthday-icon {
            font-size: 48px;
            margin-bottom: 20px;
        }
        .content {
            padding: 40px 30px;
        }
        .greeting {
            font-size: 24px;
            color: #2c3e50;
            margin-bottom: 20px;
            font-weight: bold;
        }
        .message {
            font-size: 16px;
            color: #555;
            margin-bottom: 30px;
        }
        .highlight {
            color: ${primaryColor!"#667eea"};
            font-weight: bold;
        }
        .coupon {
            background: linear-gradient(135deg, #fff9e6 0%, #fff0cc 100%);
            border: 2px dashed #ffcc00;
            border-radius: 8px;
            padding: 20px;
            margin: 25px 0;
            text-align: center;
        }
        .coupon-code {
            font-size: 28px;
            font-weight: bold;
            color: #e74c3c;
            letter-spacing: 3px;
            margin: 10px 0;
        }
        .signature {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 1px solid #eee;
            color: #666;
        }
        .footer {
            background: #f8f9fa;
            padding: 20px;
            text-align: center;
            font-size: 14px;
            color: #777;
        }
        .btn {
            display: inline-block;
            padding: 12px 30px;
            background: ${primaryColor!"#667eea"};
            color: white;
            text-decoration: none;
            border-radius: 25px;
            margin: 10px;
            font-weight: bold;
        }
        .wishes-box {
            background: #f8f9ff;
            border-left: 4px solid ${primaryColor!"#667eea"};
            padding: 15px;
            margin: 20px 0;
            font-style: italic;
        }
    </style>
</head>
<body>
<div class="email-container">
    <!-- 头部区域 -->
    <div class="header">
        <div class="birthday-icon">🎂</div>
        <h1>生日快乐！</h1>
        <p>${employeeName!"亲爱的伙伴"}，${companyName!"我们"}为您送上最真挚的祝福</p>
    </div>

    <!-- 内容区域 -->
    <div class="content">
        <div class="greeting">
            亲爱的 <span class="highlight">${employeeName!"员工"}</span>，
        </div>

        <div class="message">
            <p>在这个特别的日子里，${companyName!"公司"}全体同仁向您送上最热烈的生日祝福！</p>

            <#if workYears?? && workYears gt 0>
                <p>感谢您在过去 <span class="highlight">${workYears}</span> 年里的辛勤付出和卓越贡献。</p>
            <#else>
                <p>感谢您一直以来对公司的辛勤付出和卓越贡献。</p>
            </#if>

            <div class="wishes-box">
                <#if customMessage??>
                    ${customMessage}
                <#else>
                    愿您在新的一岁里，身体健康，工作顺利，家庭幸福，梦想成真！
                </#if>
            </div>

            <p>生日不仅仅是年龄的增长，更是智慧与阅历的积累。愿您在未来的日子里继续闪耀光芒！</p>
        </div>

        <!-- 优惠券区域 -->
        <#if couponCode?? || couponValue??>
            <div class="coupon">
                <h3>🎁 生日专属福利</h3>
                <#if couponCode??>
                    <div class="coupon-code">${couponCode}</div>
                </#if>
                <#if couponValue??>
                    <p>价值 <span class="highlight">${couponValue}</span> 的生日礼券</p>
                </#if>
                <#if couponExpiry??>
                    <p>有效期至：${couponExpiry}</p>
                </#if>
                <#if couponDescription??>
                    <p>${couponDescription}</p>
                </#if>
            </div>
        </#if>

        <!-- 行动按钮 -->
        <div style="text-align: center; margin: 30px 0;">
            <#if buttonLink??>
                <a href="${buttonLink}" class="btn">
                    <#if buttonText??>
                        ${buttonText}
                    <#else>
                        查看生日福利
                    </#if>
                </a>
            </#if>
        </div>

        <!-- 签名 -->
        <div class="signature">
            <p>此致<br>
                敬礼！</p>
            <p><strong>${companyName!"您的公司"}</strong><br>
                ${department!"人力资源部"} 敬上<br>
                ${currentYear}年${.now?string("MM")}月${.now?string("dd")}日</p>
        </div>
    </div>

    <!-- 页脚 -->
    <div class="footer">
        <p>${companyName!"公司"} - ${companySlogan!"共创美好未来"}</p>
        <p>如有任何问题，请联系：${contactEmail!"hr@company.com"} | ${contactPhone!"400-xxx-xxxx"}</p>
        <p>© ${currentYear} ${companyName!"公司"} 版权所有</p>
    </div>
</div>
</body>
</html>