module priv.captain.spi {
    requires org.mapstruct;
    requires spring.boot.autoconfigure;
    requires spring.boot;
    requires junit;
    // 如果junit5，就可以用@open注解了，不用写这么多
    exports priv.captain.basictype;
    exports priv.captain.collection.list;
    exports priv.captain.queque;
    exports priv.captain.thread;
    exports priv.captain.time;
    exports priv.captain.collection.map;
    exports priv.captain.spi; // 模块接口-导出接口包
    // requires priv.captain.spi; // 服务提供者-消费者-依赖接口模块
    provides priv.captain.spi.Search with priv.captain.spi.ESSearchImpl, priv.captain.spi.DBSearchImpl; // 服务提供者-提供服务实现
    uses priv.captain.spi.Search;// 消费者-使用服务接口 同一个模块时，只能写接口
//    uses priv.captain.spi.ESSearchImpl;// 消费者-使用服务接口
}