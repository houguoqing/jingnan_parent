import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

/*
 * @Author yaxiongliu
 **/
@SpringBootTest
public class TestEncodeToken {

    //签发token
    @Test
    public void testCompact(){
        //加密密钥
        JwtBuilder builder= Jwts.builder()
                .setId("888")   //设置唯一编号
                .setSubject("小白")//设置主题  可以是JSON数据
                .setIssuedAt(new Date())//设置签发日期
                .signWith(SignatureAlgorithm.HS256, "kkb".getBytes());//设置签名 使用HS256算法，并设置SecretKey(字符串)
        //构建 并返回一个字符串
        System.out.println( builder.compact() );//
        //eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4ODgiLCJzdWIiOiLlsI_nmb0iLCJpYXQiOjE2MTUzMDE3NTd9.DMiVlIj1VuUv7IfVrJcp8sKpRadruOcjl8e0DT9uEko
    }
    @Test
    public void testParser(){
        //
        String compactJwt = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4ODgiLCJzdWIiOiLlsI_nmb0iLCJpYXQiOjE2MTUzMDE3NTd9.DMiVlIj1VuUv7IfVrJcp8sKpRadruOcjl8e0DT9uEko";
        //解密jwt的token
        //注意：签名需要进行字节数组转换
        Claims claims = Jwts.parser().setSigningKey("kkb".getBytes()).parseClaimsJws(compactJwt).getBody();

        System.out.println("claims = " + claims);

    }
}
