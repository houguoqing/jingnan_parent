import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCrypt;

/*
 * @Author yaxiongliu
 * 测试类：测试密码加密：
 *
 **/
@SpringBootTest
public class BCryptTest {
    //加密：
    @Test
    public void testEncode(){
        //加密盐：
        String gensalt = BCrypt.gensalt();
        //加密的密码
        String password = "123456";
        String hashpw = BCrypt.hashpw(password, gensalt);
        //加密后的内容：
        System.out.println("hashpw = " + hashpw);//
        //第一次：$2a$10$YJ.aFpdf/xCAfbR21Dy/te5Pg689UQfRWSySMDS40q/abdeof4o32
        //第二次：$2a$10$NPNTxGxoe5imsOtQlRg26eIy8MFSSuiRKbXfQZs6NpIyd/Oh1vhA6
    }

    //校验：可以校验成功
    @Test
    public void testDecode(){
        boolean checkpw = BCrypt.checkpw("123456", "$2a$10$YJ.aFpdf/xCAfbR21Dy/te5Pg689UQfRWSySMDS40q/abdeof4o32");
        System.out.println("checkpw = " + checkpw);//true
    }
}
