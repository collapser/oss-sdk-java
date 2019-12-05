[![Build Status](https://travis-ci.org/HiCooper/oss-sdk-java.svg?branch=master)](https://travis-ci.org/HiCooper/oss-sdk-java)

# oss-sdk-java
java sdk for oss

## Bucket 管理

### 初始化，根据用户密钥对 初始化 `BucketManage`
````java
private static final String accessKeyId = "UmAWuGv6aC5pE7bQ6il8wO";
private static final String accessKeySecret = "URbe6TvfdF5XhEeRXiB7yewYcU5PFEe";
private final BucketManage bucketManage = new BucketManage(Auth.create(accessKeyId, accessKeySecret));
````

### 1.查询 Bucket 列表
````java
List<BucketInfoVo> bucketInfos = bucketManage.queryBucket(null);
System.out.println(JSON.toJSONString(bucketInfos));
````

### 2.创建 Bucket
````java
Boolean result = bucketManage.createBucket("hello", "oss-shanghai-1", null);
System.out.println(result);
````

### 3.更新 Bucket 权限设置
````java
Boolean result = bucketManage.updateAcl("hello", "PUBLIC_READ");
System.out.println(result);
````

### 4.删除 Bucket
````java
Boolean result = bucketManage.delete("hello");
System.out.println(result);
````

### 5.上传 对象
````java
public void createObjectTest() {
    File file = new File("./demo.png");
    if (file.isFile() && file.exists()) {
        Boolean upload = objectManage.upload("cooper", "PUBLIC_READ", null, file);
        if (upload) {
            System.out.println("上传成功！");
        }
    } else {
        System.out.println("file not exist");
    }
}
````

### 6. 字节数组格式创建 对象
````java
public void uploadObjectByteDataTest() throws IOException {
    File file = new File("./demo.png");
    if (file.isFile() && file.exists()) {
        FileInputStream inputStream = new FileInputStream(file);
        final int fileSize = inputStream.available();
        byte[] fileData = new byte[fileSize];
        int read = inputStream.read(fileData, 0, fileSize);
        if (read != fileSize) {
            throw new IOException("not enough bytes read");
        }
        inputStream.close();
        Boolean upload = objectManage.upload("cooper", "PUBLIC_READ", null, "byte_test.png", fileData);
        if (upload) {
            System.out.println("上传成功！");
        }
    } else {
        System.out.println("file not exist");
    }
}
````

### 7. base64 格式图片上传
````java
String base64Data = "data:image/jpeg;base64,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";
ObjectInfoVo vo = objectManage.upload("test", "PRIVATE", null, "bbbbb", base64Data);
if (vo != null) {
    System.out.println("上传成功！");
}
````

### 8. 获取 对象
````java
public void getObjectTest() throws IOException {
    byte[] data = objectManage.getObject("test", "NotFound-260-260.png");
    if (data != null) {
        FileOutputStream outputStream = new FileOutputStream(new File("./test2.png"));
        outputStream.write(data);
        outputStream.close();
        System.out.println("get object success");
    } else {
        System.out.println("get object fail");
    }
}
````

### 9.获取对象 临时访问 url
````java
String url = objectManage.getObjectTempAccessUrlWithExpired("cooper", "demo.png", 3600);
System.out.println(url);
````
