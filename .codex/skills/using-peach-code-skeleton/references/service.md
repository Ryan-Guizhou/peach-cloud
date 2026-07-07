# Service Layer

参考基线：

- `peach-auth/peach-auth-service/src/main/java/com/peach/auth/service/IUserService.java`
- `peach-auth/peach-auth-service/src/main/java/com/peach/auth/service/impl/UserServiceImpl.java`
- `peach-auth/peach-auth-service/src/main/java/com/peach/auth/service/impl/RouterServiceImpl.java`
- `peach-setting/peach-setting-service/src/main/java/com/peach/setting/service/impl/DictServiceImpl.java`
- `peach-fileservice/peach-fileservice-service/src/main/java/com/peach/fileservice/service/impl/FileDomainServiceImpl.java`
- `peach-generator/peach-generator-service/src/main/java/com/peach/generator/service/engine/GenDefaultTemplates.java`

当前仓库里的 service 风格不是单一模板，而是两类：

- 标准 CRUD 型：`PageHelper` + `PageInfo/PageResult`、`BeanUtils.copyProperties`、`IDGeneratorUtil.UUID()`、`DateUtil.nowTime()`、DAO 直连，见 `DictServiceImpl`。
- 领域编排型：一个公开方法协调多个 DAO/外部组件，私有方法做校验、组装、默认值和上下文解析，见 `FileDomainServiceImpl`、`UserServiceImpl`。

按以下规则编写接口：

- Service 接口负责表达能力边界、入参出参和业务语义；自定义方法的 Javadoc 优先写在接口上。
- 方法命名优先跟随模块内已有习惯，不要擅自换一套术语。
- 常见命名包括：
  - 分页：`pageList`、`typePageList`、`itemPageList`
  - 列表：`list`、`itemListByDictCode`
  - 详情：`selectById`、`typeSelectById`
  - 新增：`add`、`save`、`saveType`
  - 更新：`update`、`updateType`
  - 删除：`delById`、`deleteType`、`delete`
- 不要在接口上加 `@Service`、`@Transactional` 这类实现侧注解，除非模块已有明确先例。

按以下规则编写实现：

- 类上优先沿用 `@Slf4j`、`@Indexed`、`@Service`。
- 依赖注入优先沿用 `@Resource`，不要在同一类里混用多套注入风格。
- Spring 管理的 Service 不写 `static` 业务方法；需要复用逻辑时，优先抽私有实例方法或下沉到模块 `common` / `peach-common`。
- 允许出现 `private static final` 常量或无状态辅助字段，但不要让它们承载业务流程状态。
- 事务优先写在对外公开的 Service 方法上，统一使用 `@Transactional(rollbackFor = Exception.class)`。
- 不要把事务压在 `private` 方法上期待生效，也不要依赖 `this.xxx()` 同类自调用触发事务代理。
- 只读查询通常不加事务；写操作、跨 DAO 编排、状态流转、缓存失效联动时再加事务。
- Service 负责业务编排、补充校验、事务、缓存联动、上下文解析和 DAO 协调；不要把这些职责塞回 controller。
- 实现类中的重复逻辑优先抽成 `private` 方法，当前仓库常见的私有方法类型有：
  - `buildXxxDO(...)`
  - `requireXxx(...)`
  - `resolveXxx(...)`
  - `defaultXxx(...)`
  - `validateXxx(...)`
  - `pushXxx(...)`
- `private` 方法只承载局部组装、参数归一化、校验补充、外部调用包装和上下文获取；不要把完整主流程藏进 `private` 方法里。

参数校验与上下文规则：

- REST 已做 JSR-303 校验后，Service 仍可以补业务语义校验；当前仓库已有两种合法风格：
  - 使用 `CommonValidator`，见 `RouterServiceImpl`
  - 使用显式 `if` + `throw new RuntimeException(...)` 或直接返回，见 `FileDomainServiceImpl`、`UserServiceImpl`
- 如果模块已经使用 `CommonValidator`，优先延续，不要新旧校验风格混杂到一个局部功能里。
- 需要当前用户、组织、操作人时，优先复用现有上下文对象，例如 `CurrentContext`、`CurrentContextEntity`、`CurrentUserDO`。

DO/VO 组装规则：

- 简单 DTO -> DO 转换优先使用 `BeanUtils.copyProperties`。
- 审计字段、主键、默认状态、逻辑删除标志等，复制后显式补齐，不要假设前端会传对。
- 常见补齐动作包括：
  - `setId(IDGeneratorUtil.UUID())`
  - `setCreatedTime(DateUtil.nowTime())`
  - `setModifyTime(DateUtil.nowTime())`
  - `setIsDelete(...)`
  - `fillCreateTime(...)`
  - `fillModifyTime(...)`
- 涉及多个 DO/VO 组装步骤时，优先拆成私有 `buildXxx` / `createXxx` 方法，而不是在公开方法里堆一长段属性赋值。

缓存与横切能力：

- 如果模块已有缓存、消息推送、文件存储、外部组件协作，允许叠加在 Service 层处理。
- `@Cacheable`、`@CacheEvict`、`@Caching` 与 `@Transactional` 可以并存，风格参考 `DictServiceImpl`。
- 这类横切能力应围绕业务方法组织，不要抽成看不懂业务语义的静态工具调用链。

注释风格：

- 接口方法优先写业务语义注释，实现类不重复抄接口注释。
- 如果实现类相较接口增加了关键流程、边界、步骤说明，可以用 `{@inheritDoc}` + 补充段落，风格参考 `FileDomainServiceImpl`。
- 类注释保持项目要求的 Javadoc 标签；若模块已有 `@Description`、`@since`、`@see` 扩展标签，可在同模块内保持一致，但不要跨模块强行推广。

避免以下写法：

- 在 Service 里直接写 controller 级参数绑定逻辑。
- 在 Service 里 new 线程、自己管线程池、或把耗时逻辑塞进静态工具类。
- 对会影响事务/缓存/权限上下文的流程，拆进 `private` + `this.xxx()` 自调用。
- 把多个模块复用的业务常量散落在各自 `ServiceImpl` 中。
- 为了“复用”把只在当前方法使用的细节强行提到 `peach-common`。

案例 1：标准 CRUD Service

```java
/**
 * 字典服务实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime yyyy/M/d HH:mm
 */
@Slf4j
@Indexed
@Service
public class XxxServiceImpl implements IXxxService {

    @Resource
    private XxxDao xxxDao;

    @Override
    public PageResult<XxxVO> pageList(XxxQO qo) {
        PageInfo<XxxVO> pageInfo = PageHelper.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> xxxDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    @Override
    public XxxVO selectById(String id) {
        return xxxDao.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(XxxDTO data) {
        XxxDO entity = new XxxDO();
        BeanUtils.copyProperties(data, entity);
        entity.setId(IDGeneratorUtil.UUID());
        entity.setCreatedTime(DateUtil.nowTime());
        xxxDao.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(XxxDTO data) {
        XxxDO entity = new XxxDO();
        BeanUtils.copyProperties(data, entity);
        entity.setModifyTime(DateUtil.nowTime());
        xxxDao.updateById(entity);
    }
}
```

案例 2：领域编排型 Service

```java
/**
 * 说明领域服务职责、边界和关键流程。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime yyyy/M/d HH:mm
 */
@Slf4j
@Indexed
@Service
public class XxxDomainServiceImpl implements IXxxDomainService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DateUtil.TIME_PATTERN);

    @Resource
    private XxxDao xxxDao;

    @Resource
    private ExternalGateway externalGateway;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XxxResultVO execute(XxxCommandDTO data) {
        validateData(data);
        XxxDO exists = requireActiveEntity(data.getBizId());
        ExternalResult result = callExternal(exists, data);
        XxxDO update = buildUpdateDO(exists, result);
        xxxDao.updateById(update);
        return buildResult(update, result);
    }

    private void validateData(XxxCommandDTO data) {
        if (data == null) {
            throw new RuntimeException("request data is empty");
        }
    }

    private XxxDO requireActiveEntity(String bizId) {
        XxxDO entity = xxxDao.selectById(bizId);
        if (entity == null) {
            throw new RuntimeException("entity not found");
        }
        return entity;
    }

    private ExternalResult callExternal(XxxDO entity, XxxCommandDTO data) {
        return externalGateway.execute(entity.getBizCode(), data.getOperator());
    }

    private XxxDO buildUpdateDO(XxxDO entity, ExternalResult result) {
        XxxDO update = new XxxDO();
        update.setId(entity.getId());
        update.setStatus(result.getStatus());
        update.setModifyTime(DateUtil.nowTime());
        return update;
    }

    private XxxResultVO buildResult(XxxDO update, ExternalResult result) {
        XxxResultVO vo = new XxxResultVO();
        vo.setId(update.getId());
        vo.setStatus(result.getStatus());
        return vo;
    }
}
```

提交前检查：

- 是否沿用了当前模块已有命名，而不是发明新术语。
- 是否统一使用 `@Resource`、`@Transactional(rollbackFor = Exception.class)`、`BeanUtils.copyProperties` 等项目内常见写法。
- 是否把事务、缓存、外部调用和状态流转放在公开方法上。
- 是否把大段组装逻辑拆成命名清晰的私有 helper。
- 是否把真正通用的常量/方法提到模块 `common` 或 `peach-common`，而不是继续堆在 `ServiceImpl`。
