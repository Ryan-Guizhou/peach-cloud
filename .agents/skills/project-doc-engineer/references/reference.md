# Reference Pattern

Reference 负责“查”，要求完整、精确、稳定，不承担教程叙事。

适合内容：

- Configuration Properties：key / type / default / constraints / meaning。
- Public API / SPI：contract / parameters / return / exception / lifecycle。
- State / Error / Event / Topic / Metric 的精确定义。
- Provider / capability matrix。
- Troubleshooting：Symptom -> Evidence -> Cause -> Fix -> Verify。

配置默认值必须来自代码。不存在默认值就明确写“无默认值/必须配置”，不要猜。

排障只收录真实可发生的故障模式；不要用“重启服务试试”替代诊断路径。
