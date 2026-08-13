// #!/usr/bin/env node
//
// import { spawnSync } from "node:child_process";
// import { readFileSync } from "node:fs";
//
// const configPath = ".codex/config.toml";
// const config = readFileSync(configPath, "utf8");
// const errors = [];
// const warnings = [];
//
// if (config.charCodeAt(0) === 0xfeff) {
//   errors.push(`${configPath}: 包含 UTF-8 BOM`);
// }
// if (config.includes("@latest")) {
//   errors.push(`${configPath}: MCP 依赖不得使用 @latest`);
// }
// if (config.includes("@modelcontextprotocol/server-github")) {
//   errors.push(`${configPath}: 使用了已停止维护的 GitHub MCP`);
// }
// if (!config.includes("ghcr.io/github/github-mcp-server:")) {
//   errors.push(`${configPath}: 未配置固定版本的官方 GitHub MCP`);
// }
// if (!config.includes('AGENTMEMORY_TOOLS = "core"')) {
//   errors.push(`${configPath}: agentmemory 日常工具集必须为 core`);
// }
//
// function checkCommand(command, args) {
//   const isWindowsNpx = process.platform === "win32" && command === "npx";
//   const executable = isWindowsNpx ? "cmd.exe" : command;
//   const commandArgs = isWindowsNpx ? ["/d", "/s", "/c", "npx --version"] : args;
//   const result = spawnSync(executable, commandArgs, { encoding: "utf8" });
//   if (result.error || result.status !== 0) {
//     warnings.push(`未检测到可用命令 ${command}；依赖该命令的 MCP 将无法启动`);
//   }
// }
//
// checkCommand("node", ["--version"]);
// checkCommand("npx", ["--version"]);
// checkCommand("docker", ["--version"]);
//
// for (const warning of warnings) {
//   console.warn(`WARN: ${warning}`);
// }
// if (errors.length > 0) {
//   for (const error of errors) {
//     console.error(`ERROR: ${error}`);
//   }
//   process.exit(1);
// }
// console.log("MCP 静态配置检查通过；WARN 项需在使用对应 MCP 前处理。");
