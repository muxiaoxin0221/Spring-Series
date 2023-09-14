# Uploading Files

## 创建 File Upload Controller

```java
@Controller
public class FileUploadController {

  private final StorageService storageService;

  @Autowired
  public FileUploadController(StorageService storageService) {
    this.storageService = storageService;
  }

  @GetMapping("/")
  public String listUploadedFiles(Model model) throws IOException {

    model.addAttribute("files", storageService.loadAll().map(
            path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                "serveFile", path.getFileName().toString()).build().toUri().toString())
        .collect(Collectors.toList()));

    return "uploadForm";
  }

  @GetMapping("/files/{filename:.+}")
  @ResponseBody
  public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

    Resource file = storageService.loadAsResource(filename);
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + file.getFilename() + "\"").body(file);
  }

  @PostMapping("/")
  public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {

    storageService.store(file);
    redirectAttributes.addFlashAttribute("message",
        "You successfully uploaded " + file.getOriginalFilename() + "!");
    return "redirect:/";
  }

  @ExceptionHandler(StorageFileNotFoundException.class)
  public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
    return ResponseEntity.notFound().build();
  }

}
```

Controller主要提供了3个API:

**GET: localhost:8080/** 对应listUploadedFiles方法显示uploadForm.html,通过storage service将所有以上传文件添加至model的files属性。uploadForm会在浏览器中展示files中的内容。

**POST: localhost:8080/** 为上传页面中已经选择的文件。

**GET localhost:8080/files/{filename:.+}** 点击页面中列出的已上传文件，浏览器会下载此文件。

## 上传页面

```html
<html xmlns:th="https://www.thymeleaf.org">
<body>

<div th:if="${message}">
    <h2 th:text="${message}"/>
</div>

<div>
    <form method="POST" enctype="multipart/form-data" action="/">
        <table>
            <tr>
                <td>File to upload:</td>
                <td><input type="file" name="file"/></td>
            </tr>
            <tr>
                <td></td>
                <td><input type="submit" value="Upload"/></td>
            </tr>
        </table>
    </form>
</div>

<div>
    <ul>
        <li th:each="file : ${files}">
            <a th:href="${file}" th:text="${file}"/>
        </li>
    </ul>
</div>

</body>
</html>
```
html中的参数命名，GET/POST方法的调用与Controller中一一对应。

## Storage Service实现

这个Demo的storage会将文件存储到本地Filesystem。
```java
public interface StorageService {

  void init();

  void store(MultipartFile file);

  Stream<Path> loadAll();

  Path load(String filename);

  Resource loadAsResource(String filename);

  void deleteAll();

}
```

```java
@Service
public class FileSystemStorageService implements StorageService {

  private final Path rootLocation;

  @Autowired
  public FileSystemStorageService(StorageProperties properties) {
    this.rootLocation = Paths.get(properties.getLocation());
  }

  @Override
  public void init() {
    try {
      Files.createDirectories(rootLocation);
    } catch (IOException e) {
      throw new StorageException("Could not initialize storage", e);
    }
  }

  @Override
  public void store(MultipartFile file) {
    if (file.isEmpty()) {
      throw new StorageException("Failed to store empty file.");
    }

    Path destinationFile = this.rootLocation
        .resolve(Paths.get(Objects.requireNonNull(file.getOriginalFilename())))
        .normalize()
        .toAbsolutePath();
    if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
      throw new StorageException("Cannot store file outside current directory.");
    }

    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new StorageException("Failed to store file.", e);
    }
  }

  @Override
  public Stream<Path> loadAll() {
    try {
      return Files.walk(this.rootLocation, 1)
          .filter(path -> !path.equals(this.rootLocation))
          .map(this.rootLocation::relativize);
    } catch (IOException e) {
      throw new StorageException("Failed to read stored files", e);
    }
  }

  @Override
  public Path load(String filename) {
    return rootLocation.resolve(filename);
  }

  @Override
  public Resource loadAsResource(String filename) {
    try {
      Path file = load(filename);
      Resource resource = new UrlResource(file.toUri());
      if (resource.exists() || resource.isReadable()) {
        return resource;
      } else {
        throw new StorageFileNotFoundException("Could not read file: " + filename);
      }
    } catch (MalformedURLException e) {
      throw new StorageFileNotFoundException("Could not read file: " + filename, e);
    }
  }

  @Override
  public void deleteAll() {
    FileSystemUtils.deleteRecursively(rootLocation.toFile());
  }

}
```

## File Upload参数配置

```xml
spring.servlet.multipart.max-file-size=128KB
spring.servlet.multipart.max-request-size=128KB
```

上传的文件不能超过128K。

## 在Application中初始化storage service

```java
@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class UploadingFilesApplication {

  public static void main(String[] args) {
    SpringApplication.run(UploadingFilesApplication.class, args);
  }

  @Bean
  CommandLineRunner init(StorageService storageService) {
    return (args) -> {
      storageService.deleteAll();
      storageService.init();
    };
  }
}
```

demo会清空上传文件夹，然后重新创建该目录。