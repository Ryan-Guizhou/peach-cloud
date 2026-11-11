
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { basename, dirname, join, relative, resolve } from "node:path";

const ROOT = resolve(import.meta.dirname, "..");
function kebab(name){return name.replace(/([A-Z])([A-Z][a-z])/g,"$1-$2").replace(/([a-z0-9])([A-Z])/g,"$1-$2").toLowerCase();}

function inferDefault(field, constants = {}){
 const init=(field.init||"").replace(/;\s*$/,"").trim();
 if(Object.prototype.hasOwnProperty.call(constants, init))return constants[init];
 if(!init){if(field.type==="boolean")return false;if(["int","long","double","float"].includes(field.type))return 0;return undefined;}
 if(init==="true"||init==="false")return init==="true";
 if(/^"(?:\\.|[^"\\])*"$/.test(init)){return init.slice(1,-1);}
 if(/^[-\d_]+(?:\.\d+)?[lLfFdD]?$/.test(init))return Number(init.replaceAll("_","").replace(/[lLfFdD]$/,""));
 if(/^[-\d_Ll\s*+/.]+$/.test(init)&&/[\d]\s*[\*+]/.test(init)){
  const vals=init.split("*").map(x=>x.trim().replace(/[lL]$/,"").replaceAll("_",""));
  if(vals.every(x=>/^\d+$/.test(x)))return vals.reduce((a,b)=>a*Number(b),1);
 }
 const dur=init.match(/^Duration\.of(Millis|Seconds|Minutes|Hours|Days)\((\d+)[lL]?\)$/);
 if(dur){return dur[2]+({Millis:"ms",Seconds:"s",Minutes:"m",Hours:"h",Days:"d"}[dur[1]]);}
 if(init==="Duration.ZERO")return "0ms";
 if(!["boolean","int","long","float","double","Integer","Long","Double","Float","String"].includes(field.type)){
  const en=init.match(/^([\w.]+)\.([A-Z][A-Z0-9_]*)$/);
  if(en&&!["Font"].includes(en[1]))return en[2];
 }
 return undefined;
}

function javaType(type,pack,root,classes,imports){
 const primitive={boolean:"java.lang.Boolean",Boolean:"java.lang.Boolean",int:"java.lang.Integer",Integer:"java.lang.Integer",long:"java.lang.Long",Long:"java.lang.Long",double:"java.lang.Double",Double:"java.lang.Double",float:"java.lang.Float",Float:"java.lang.Float",String:"java.lang.String",Duration:"java.time.Duration"};
 if(primitive[type])return primitive[type];
 if(type.startsWith("Map<"))return "java.util.Map";
 if(type.startsWith("Set<"))return "java.util.Set";
 if(type.startsWith("List<"))return "java.util.List";
 if(classes.has(type))return pack+"."+root+"$"+type;
 if(type.includes(".")){
  const [left,...rest]=type.split(".");
  const found=imports[left];
  return found?found+"$"+rest.join("$"):type;
 }
 return imports[type]??(pack+"."+type);
}

function propertyModel(source,file,constants = {}){
 const ann=source.match(/@ConfigurationProperties\s*\(\s*(?:prefix|value)\s*=\s*("[^"]+"|[\w.]+)\s*\)/);
 if(!ann)return [];
 const symbolic={ "CaptchaConst.CAPTCHA_SUFFIX":"peach.captcha","RedisStreamContant.REDIS_STREAM_PREFIX":"peach.redis.stream","DelayQueueProperties.PREFIX":"peach.delay.queue"};
 const prefix=ann[1].startsWith('"')?ann[1].slice(1,-1):symbolic[ann[1]];
 if(!prefix)throw Error("Unresolved @ConfigurationProperties prefix: "+file+" "+ann[1]);
 const pack=source.match(/^\s*package\s+([\w.]+)\s*;/m)?.[1]??"";
 const imports={};
 for(const match of source.matchAll(/^\s*import\s+([\w.]+)\s*;/gm)){const fq=match[1];imports[fq.split(".").at(-1)]=fq;}
 const clsName=source.match(/^\s*public\s+(?:final\s+)?class\s+(\w+)/m)?.[1];
 if(!clsName)throw Error("Expected a public configuration class: "+file);
 const classes=new Map();
 let depth=0,stack=[];
 for(const line of source.split("\n")){
  const classMatch=line.match(/^\s*(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?class\s+(\w+)/);
  if(classMatch){const name=classMatch[1];if(!classes.has(name))classes.set(name,[]);stack.push({name,level:depth+1});}
  const field=line.match(/^\s*private\s+(?!static\b)(?:final\s+)?(.+?)\s+([A-Za-z]\w*)\s*(?:=\s*(.*)|;)/);
  if(field&&stack.length){
   const type=field[1].trim(),name=field[2],init=field[3]??"";
   if(!type.includes(" ")||type.includes("<"))classes.get(stack.at(-1).name).push({type,name,init});
  }
  const noStrings=line.replace(/"(?:\\.|[^"\\])*"/g,'""');
  depth+=(noStrings.match(/\{/g)||[]).length-(noStrings.match(/\}/g)||[]).length;
  while(stack.length&&depth<stack.at(-1).level)stack.pop();
 }
 const props=[],seen=new Set();
 function visit(cls,pfx,parents){
  if(parents.includes(cls))return;
  for(const fld of classes.get(cls)??[]){
   const name=pfx+"."+kebab(fld.name);
   if(seen.has(name))continue;
   seen.add(name);
   const model={name,type:javaType(fld.type,pack,clsName,classes,imports)};
   const def=inferDefault(fld,constants);
   if(def!==undefined)model.defaultValue=def;
   props.push(model);
   let nested=classes.has(fld.type)?fld.type:null;
   if(!nested&&fld.type.startsWith("Map<")) {
    const m=fld.type.match(/,\s*(\w+)\s*>/);
    if(m&&classes.has(m[1]))nested=m[1];
   }
   if(nested)visit(nested,name+(fld.type.startsWith("Map<")?".*":""),[...parents,cls]);
  }
 }
 visit(clsName,prefix,[]);
 return props;
}

function inlineConfigurationProperties(source,constants = {}){
 const properties = new Map();
 for(const match of source.matchAll(/@ConditionalOnProperty\s*\(([\s\S]*?)\)/g)){
  const annotation=match[1];
  const pref=annotation.match(/\bprefix\s*=\s*("[^"]+"|[\w.]+)/)?.[1]??"";
  const prefix=pref.startsWith('"')?pref.slice(1,-1):constants[pref]??"";
  const rawNames=annotation.match(/\b(?:name|value)\s*=\s*(\{[^}]*\}|"[^"]+")/)?.[1];
  if(!rawNames)continue;
  const names=[...rawNames.matchAll(/"([^"]+)"/g)].map(x=>x[1]);
  for(const n of names){
   const full=(prefix?prefix+".":"")+n;
   if(!full.startsWith("peach."))continue;
   const having=annotation.match(/\bhavingValue\s*=\s*"([^"]+)"/)?.[1];
   const isBoolean=having==="true"||having==="false"||(!having&&/(?:^|[.-])enable(?:d)?$/.test(n));
   const property={name:full,type:isBoolean?"java.lang.Boolean":"java.lang.String",
     description:describe(full)};
   if(isBoolean)property.defaultValue=/\bmatchIfMissing\s*=\s*true\b/.test(annotation);
   properties.set(full,property);
  }
 }
 for(const match of source.matchAll(/@Value\s*\(\s*"\$\{(peach\.[^}:]+)(?::([^}]+))?\}"\s*\)/g)){
  const name=match[1];const raw=match[2];
  const property={name,type:"java.lang.String",description:describe(name)};
  if(raw!==undefined){
   if(/^(?:true|false)$/.test(raw)){property.type="java.lang.Boolean";property.defaultValue=raw==="true";}
   else if(/^\d+$/.test(raw)){property.type="java.lang.Integer";property.defaultValue=Number(raw);}
   else property.defaultValue=raw;
  }
  properties.set(name,property);
 }
 return [...properties.values()];
}

function describe(name,def){
 const parts=name.split(".");
 const item=parts.at(-1).replaceAll("-"," ");
 if(/password|secret|access.key|token|credential/i.test(name))return "Configures "+item+"; supply sensitive values from a secure configuration source.";
 if(/enabled$/.test(name))return "Enables or disables "+parts.slice(0,-1).join(".")+" functionality.";
 if(/timeout|interval|wait|lease|retention|expire|delay/i.test(item))return "Configures "+item+" for "+parts.slice(0,-1).join(".")+"; check units and bounds.";
 if(/max|min|limit|capacity|count|size/i.test(item))return "Sets the "+item+" limit for "+parts.slice(0,-1).join(".")+"; validate capacity and memory budgets.";
 if(/\.providers$|\.named$|\.groups$/.test(name))return "Configures named entries; add a map key before the nested properties.";
 return "Configures "+item+" for "+parts.slice(0,-1).join(".")+".";
}

function mergeMetadata(existing,expected){
 const data={...existing};
 const all=new Map((Array.isArray(existing.properties)?existing.properties:[]).map(v=>[v.name,{...v}]));
 for(const p of expected){
  const old=all.get(p.name)??{};
  const v={name:p.name,...old,type:(["java.lang.Boolean","java.lang.Integer","java.lang.Long","java.lang.Double","java.lang.Float","java.lang.String","java.time.Duration"].includes(p.type)?p.type:(old.type??p.type)),description:old.description?.trim()||describe(p.name,p.defaultValue)};
  if(Object.prototype.hasOwnProperty.call(p,"defaultValue"))v.defaultValue=p.defaultValue;
  all.set(p.name,v);
 }
 data.properties=[...all.values()].sort((x,y)=>x.name.localeCompare(y.name,"en"));
 if(!Array.isArray(data.hints))data.hints=[];
 return data;
}

const AUTO_ROOTS = ["peach-component", "peach-middleware"];
const META_RELATIVE = "src/main/resources/META-INF/additional-spring-configuration-metadata.json";
const FIX = process.argv.includes("--fix");
const errors = [];

function walk(directory, callback) {
    if (!existsSync(directory)) return;
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
        const name = join(directory, entry.name);
        if (entry.isDirectory()) walk(name, callback);
        else callback(name);
    }
}

function checkOne(moduleDir) {
    const javaDir = join(moduleDir, "src/main/java");
    const configFiles = [];
    const sources = [];
    walk(javaDir, (file) => {
        if (file.endsWith(".java")) {
            const source = readFileSync(file, "utf8");
            const javaPackage = source.match(/^\s*package\s+([\w.]+)\s*;/m)?.[1];
            sources.push({ file, source, javaPackage });
        }
    });
    const constants = {};
    for (const current of sources) {
        const javaClass = current.source.match(/^\s*public\s+(?:final\s+)?class\s+(\w+)/m)?.[1];
        if (!javaClass) continue;
        const literalPattern = /^\s*public\s+static\s+final\s+(?:String|Integer|Long|int|long|boolean)\s+([A-Z][A-Z0-9_]*)\s*=\s*("[^"]*"|\d+[lL]?|true|false)\s*;/gm;
        for (const match of current.source.matchAll(literalPattern)) {
            const raw = match[2];
            constants[javaClass + "." + match[1]] = raw.startsWith('"') ? raw.slice(1, -1)
                    : raw === "true" ? true : raw === "false" ? false
                    : Number(raw.replace(/[lL]$/, ""));
        }
    }
    for (const current of sources) {
        if (current.source.includes("@ConfigurationProperties(")) {
            // 同包的嵌套配置对象可能声明在另一份 Java 文件中（如 MultiCache）。
            const siblings = sources.filter((other) => other.file !== current.file &&
                    other.javaPackage === current.javaPackage).map((other) => other.source).join("\n");
            const expected = propertyModel(current.source + "\n" + siblings, relative(ROOT, current.file), constants);
            if (expected.length) configFiles.push(...expected);
        }
    }
    const inlineProperties = new Map();
    for (const current of sources) {
        for (const property of inlineConfigurationProperties(current.source, constants)) {
            inlineProperties.set(property.name, property);
        }
    }
    const declaredNames = new Set(configFiles.map((property) => property.name));
    for (const property of inlineProperties.values()) {
        if (!declaredNames.has(property.name)) configFiles.push(property);
    }
    if (configFiles.length &&
            !readFileSync(join(moduleDir, "pom.xml"), "utf8")
                .includes("<artifactId>spring-boot-configuration-processor</artifactId>")) {
        errors.push(relative(ROOT, moduleDir) +
            ": @ConfigurationProperties modules must declare spring-boot-configuration-processor");
    }
    const metaFile = join(moduleDir, META_RELATIVE);
    const fileName = relative(ROOT, metaFile);
    let existing = {};
    if (existsSync(metaFile)) {
        try {
            existing = JSON.parse(readFileSync(metaFile, "utf8"));
        } catch (error) {
            errors.push(fileName + ": invalid JSON: " + error.message);
            return;
        }
    } else if (!FIX) {
        errors.push(fileName + ": every autoconfigure module must contain additional metadata");
        return;
    }
    if (FIX) {
        const next = mergeMetadata(existing, configFiles);
        mkdirSync(dirname(metaFile), { recursive: true });
        writeFileSync(metaFile, JSON.stringify(next, null, 2) + "\n", "utf8");
        return;
    }
    if (!Array.isArray(existing.properties)) {
        errors.push(fileName + ": properties must be an array (empty is valid if there are no configurable properties)");
        return;
    }
    const known = new Map();
    for (const prop of existing.properties) {
        if (typeof prop.name !== "string" || !/^[a-z0-9][a-z0-9.*-]*(\.[a-z0-9*-]+)*$/.test(prop.name)) {
            errors.push(fileName + ": invalid kebab-case property name: " + prop.name);
            continue;
        }
        if (known.has(prop.name)) errors.push(fileName + ": duplicate property: " + prop.name);
        if (typeof prop.type !== "string" || !prop.type.trim()) errors.push(fileName + ": missing type: " + prop.name);
        if (typeof prop.description !== "string" || !prop.description.trim()) errors.push(fileName + ": missing description: " + prop.name);
        known.set(prop.name, prop);
    }
    for (const expected of configFiles) {
        const actual = known.get(expected.name);
        if (!actual) {
            errors.push(fileName + ": missing declared @ConfigurationProperties field: " + expected.name);
            continue;
        }
        if (Object.prototype.hasOwnProperty.call(expected, "defaultValue")) {
            if (!Object.prototype.hasOwnProperty.call(actual, "defaultValue") ||
                    JSON.stringify(actual.defaultValue) !== JSON.stringify(expected.defaultValue)) {
                errors.push(fileName + ": default value drift: " + expected.name +
                    " expected=" + JSON.stringify(expected.defaultValue) +
                    " actual=" + JSON.stringify(actual.defaultValue));
            }
        }
        if (["java.lang.Boolean","java.lang.Integer","java.lang.Long","java.lang.Double","java.lang.Float","java.lang.String","java.time.Duration"].includes(expected.type) &&
                actual.type !== expected.type) {
            errors.push(fileName + ": property type drift: " + expected.name + " expected=" + expected.type + " actual=" + actual.type);
        }
    }
}

function findAutoconfigProjects(directory) {
    walk(directory, (file) => {
        if (basename(file) === "pom.xml" && basename(dirname(file)).endsWith("-autoconfigure")) {
            checkOne(dirname(file));
        }
    });
}

function selfTest() {
    const example = 'package x.y;\n@ConfigurationProperties(prefix = "peach.sample")\n' +
        'public class ExampleProperties {\n private boolean enabled = true;\n' +
        'private final Group group = new Group();\npublic static class Group {\n' +
        'private Duration waitTimeout = Duration.ofSeconds(5);\nprivate int maxSize = 20;\nprivate String topic = ExampleConstants.TOPIC;\n}\n}';
    const fields = propertyModel(example, "ExampleProperties.java", {
        "ExampleConstants.TOPIC": "test-topic"
    });
    const switches=inlineConfigurationProperties('@ConditionalOnProperty(prefix = "peach.sample", name = "toggle", havingValue = "true", matchIfMissing = true)' +
            '@Value("${peach.sample.size:10}")');
    if (!switches.some((x) => x.name === "peach.sample.toggle" && x.defaultValue === true) ||
            !switches.some((x) => x.name === "peach.sample.size" && x.defaultValue === 10)) {
        throw new Error("metadata inline property self-test failed");
    }
    if (!fields.some((x) => x.name === "peach.sample.group.wait-timeout" && x.defaultValue === "5s") ||
            !fields.some((x) => x.name === "peach.sample.enabled" && x.defaultValue === true) ||
            !fields.some((x) => x.name === "peach.sample.group.topic" && x.defaultValue === "test-topic")) {
        throw new Error("metadata parser nested/default-value self-test failed");
    }
}
selfTest();
for (const root of AUTO_ROOTS) findAutoconfigProjects(join(ROOT, root));
if (errors.length) {
    for (const error of errors) console.error("[spring-metadata] " + error);
    process.exit(1);
}
console.log("[spring-metadata] " + (FIX ? "generated" : "validated") +
    " Starter configuration metadata, defaults and source-property coverage.");
