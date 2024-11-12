module priv.captain.spi{
    requires junit;
    requires org.mapstruct;
    requires spring.boot.autoconfigure;
    requires spring.boot;
    exports priv.captain.spi; // 模块接口-导出接口包
    // requires priv.captain.spi; // 服务提供者-消费者-依赖接口模块
    provides priv.captain.spi.Search with priv.captain.spi.ESSearchImpl,priv.captain.spi.DBSearchImpl; // 服务提供者-提供服务实现
    uses priv.captain.spi.Search;// 消费者-使用服务接口 同一个模块时，只能写接口
//    uses priv.captain.spi.ESSearchImpl;// 消费者-使用服务接口
}