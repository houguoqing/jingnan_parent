import com.jn.util.IdWorker;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

/*
 * @Author yaxiongliu
 **/
@SpringBootTest
public class TestIdWorker {
    //测试分布式id生成器
    @Test
    public void test(){
        //创建分布式id生成器对象
        IdWorker idWorker=new IdWorker(1,1);

        for(int i=0;i<10000;i++){
            long id = idWorker.nextId();//生成1个id
            System.out.println(id);
        }
    }
}
