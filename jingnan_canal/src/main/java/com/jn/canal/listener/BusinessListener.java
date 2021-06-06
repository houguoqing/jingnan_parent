package com.jn.canal.listener;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.xpand.starter.canal.annotation.CanalEventListener;
import com.xpand.starter.canal.annotation.ListenPoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 业务库中，广告表数据变更的监听器：
 * 一旦表发生数据变化，就会写binlog，然后同步到伪从节点Canal
 * Canal服务会将数据推送到监听的微服务上：jingnan_canal
 *
 */
@CanalEventListener
public class BusinessListener {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * @param eventType 监听事件的类型：新增、修改、删除、查询...
     * @param rowData 监听到时间的对应的数据
     */
    @ListenPoint(schema = "jingnan_business", table = {"tb_ad"})
    public void adUpdate(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        System.err.println("广告数据发生变化");
        //  1.判断如果不为新增、修改、删除，返回，则不发送MQ消息
        if (eventType.equals(CanalEntry.EventType.CREATE) || eventType.equals(CanalEntry.EventType.UPDATE)
                || eventType.equals(CanalEntry.EventType.DELETE)){

            //2. 发送MQ消息：修改数据
            for(CanalEntry.Column column: rowData.getAfterColumnsList()) {
                if(column.getName().equals("position")){
                    System.out.println("发送消息到mq  ad_update_queue:"+column.getValue());
                    String position = column.getValue();//广告位置
                    rabbitTemplate.convertAndSend("","ad_update_queue", position);  //发送消息到mq
                    break;
                }
            }
        }
    }
}
