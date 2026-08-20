#!/usr/bin/env node

import { execFileSync } from "child_process";
import { existsSync, readFileSync, writeFileSync } from "fs";
import { extname } from "path";
import { TextDecoder } from "util";

const UTF8_BOM = Buffer.from([0xef, 0xbb, 0xbf]);
const decoder = new TextDecoder("utf-8", { fatal: true });
const fixBom = process.argv.includes("--fix-bom");

const textExtensions = new Set([
    ".bat",
    ".cmd",
    ".css",
    ".csv",
    ".editorconfig",
    ".html",
    ".java",
    ".js",
    ".jsx",
    ".json",
    ".kt",
    ".kts",
    ".md",
    ".mjs",
    ".properties",
    ".ps1",
    ".scss",
    ".sh",
    ".sql",
    ".svg",
    ".toml",
    ".ts",
    ".tsx",
    ".txt",
    ".vue",
    ".xml",
    ".yaml",
    ".yml",
]);

const textNames = new Set([
    ".gitattributes",
    ".gitignore",
    "AGENTS.md",
    "Dockerfile",
    "LICENSE",
    "NOTICE",
    "pom.xml",
]);

const mojibakeMarkers = [
    "\uFFFD",
    String.fromCodePoint(0x951f, 0x65a4, 0x62f7),
    String.fromCodePoint(0x00ef, 0x00bb, 0x00bf),
];

function trackedFiles() {
    const output = execFileSync(
        "git",
        [
            "ls-files",
            "--cached",
            "--others",
            "--exclude-standard",
            "-z",
        ]
    );

    return output
        .toString("utf8")
        .split("\0")
        .filter(Boolean);
}

function isTextFile(file) {
    const normalized = file.replace(/\\/g, "/");
    const name = normalized.slice(normalized.lastIndexOf("/") + 1);

    return (
        textNames.has(name) ||
        textExtensions.has(extname(name).toLowerCase())
    );
}

const failures = [];

let fixedCount = 0;
let checkedCount = 0;

for (const file of trackedFiles()) {
    if (!isTextFile(file) || !existsSync(file)) {
        continue;
    }

    checkedCount += 1;

    let bytes = readFileSync(file);

    if (bytes.subarray(0, 3).equals(UTF8_BOM)) {
        if (fixBom) {
            bytes = bytes.subarray(3);
            writeFileSync(file, bytes);
            fixedCount += 1;
        } else {
            failures.push(`${file}: 包含 UTF-8 BOM`);
            continue;
        }
    }

    let content;

    try {
        content = decoder.decode(bytes);
    } catch {
        failures.push(`${file}: 不是合法的 UTF-8`);
        continue;
    }

    for (const marker of mojibakeMarkers) {
        if (content.includes(marker)) {
            failures.push(
                `${file}: 包含疑似乱码标记 ${JSON.stringify(marker)}`
            );
        }
    }
}

if (fixedCount > 0) {
    console.log(
        `已移除 ${fixedCount} 个文本文件的 UTF-8 BOM。`
    );
}

if (failures.length > 0) {
    console.error(
        `UTF-8 检查失败（${failures.length} 项）：`
    );

    for (const failure of failures) {
        console.error(`- ${failure}`);
    }

    process.exit(1);
}

console.log(
    `UTF-8 检查通过：${checkedCount} 个受版本控制的文本文件均为 UTF-8 无 BOM。`
);
