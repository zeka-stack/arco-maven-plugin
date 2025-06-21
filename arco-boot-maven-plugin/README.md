## 插件机制实现

1. IoC 方式: 母体应用声明接口, 外部插件实现接口并且通过 @Component @Service 或其他注解让 Spring 容器管理, 母体应用通过 @Resource @Autowired
   来注入.
2. SPI 方式: 母体应用声明接口, 外部插件实现接口并且配置于 META-INF/services/ 下, 母体应用通过 ServiceLoader 加载接口的实现类.
3. AOP 方式: 外部插件通过 Spring Aspect 技术实现对母体应用的切面拦截.
