# Building A RESTful Web Service

## 创建一个资源表示类

当我们访问/greeting时，需要返回一个JSON格式的greeting，如下：

```json
{
  "id": 1,
  "content": "Hello, World!"
}
```
相应的我们创建一个java record类。
```java
public record Greeting(long id, String content) {
}
```

## 创建 Controller

```java
@RestController
public class HelloController {

  private static final String template = "Hello, %s!";
  private final AtomicLong counter = new AtomicLong();

  @GetMapping("/greeting")
  public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
    return new Greeting(counter.incrementAndGet(), String.format(template, name));
  }

}
```
我们可以看到这次greeting返回的是一个Greeting对象。那为什么这个对象在浏览器中会直接输出JSON格式呢？

这里Spring会用Jackson Json自动将Greeting对象转换成JSON格式。因为我们用了注解@RestController,所以转换后的JSON字符串为直接塞进Response Body中。

