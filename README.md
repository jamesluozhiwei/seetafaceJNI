# seetafaceJNI

#### 项目介绍
基于中科院seetaface2进行封装的JAVA人脸识别算法库，支持人脸识别、1:1比对、1:N比对。
seetaface2：https://github.com/seetaface/SeetaFaceEngine2

#### 环境配置
1、下载model（ https://pan.baidu.com/s/1HJj8PEnv3SOu6ZxVpAHPXg ） 文件到本地，并解压出来；

2、下载doc目录中对应的lib包到本地并解压：Windows(64位)环境下载lib-win-x64.zip、Linux(64位)下载lib-linux-x64.tar.bz2，Linux环境还需要安装依赖库；

- ubuntu 16.04 可使用一下命令
```
sudo apt-get install libopenblas-dev libprotobuf-dev libssl-dev
```
- 其他见 https://my.oschina.net/u/1580184/blog/3042404 

3、将src/main/resources/中的seetaface.properties文件放到项目的resources根目录中；

```properties
#linux系统中依赖的lib名称
libs=holiday,SeetaFaceDetector200,SeetaPointDetector200,SeetaFaceRecognizer200,SeetaFaceCropper200,SeetaFace2JNI
#Windows系统中依赖的lib名称
#libs=libgcc_s_sjlj-1,libeay32,libquadmath-0,ssleay32,libgfortran-3,libopenblas,holiday,SeetaFaceDetector200,SeetaPointDetector200,SeetaFaceRecognizer200,SeetaFaceCropper200,SeetaFace2JNI

#lib存放目录
libs.path=/usr/local/seetaface2/lib
#model存放目录
bindata.dir=/usr/local/seetaface2/bindata

```

5、将seetafaceJNI-1.1.jar和依赖包导入到项目中，pom如下:

可以引入源码也可以引入jar包

```xml
       <dependency>
            <groupId>com.lzw</groupId>
            <artifactId>seetafaceJNI</artifactId>
            <version>1.1</version>
            <!--<scope>system</scope>-->
            <!--<systemPath>${project.basedir}/lib/seetafaceJNI-1.1.jar</systemPath>-->
       </dependency>
```

如果引入jar包，maven打包时请将jar包添加的自己本地的maven仓库，否则jar包不会被打包进去

```
mvn install:install-file -DgroupId=com.lzw -DartifactId=seetafaceJNI -Dversion=1.1 -Dpackaging=jar -Dfile=seetafaceJNI-1.1.jar
```

6、调用FaceHelper中的方法。


#### 使用方法
所有方法都封装到了FaceHelper工具类中
```java
    /**
     * 人脸比对
     *
     * @param img1
     * @param img2
     * @return 相似度
     */
    float compare(File img1, File img2);
    float compare(byte[] img1, byte[] img2);
    float compare(BufferedImage image1, BufferedImage image2);
    
    /**
     * 注册人脸（会裁剪图片）
     * @param img 人脸照片
     * @return 人脸在人脸库的下标(需自己存储、人脸搜索时返回下标及相似度)
     */
    int register(byte[] img);
    /**
     * 注册人脸（不裁剪图片）
     * @param image 人脸照片
     * @return 同上
     */
    int register(BufferedImage image)
    
    /**
     * 搜索人脸
     * @param img 人脸照片
     * @return 返回最相似的人脸在人脸库的下标及相似度
     */
    RecognizeResult search(byte[] img);
    RecognizeResult search(BufferedImage image);
    
    /**
     * 人脸提取（裁剪）
     * @param img
     * @return return cropped face
     */
    BufferedImage crop(byte[] img);
    BufferedImage crop(BufferedImage image);
    
    /**
     * 人脸识别
     * @param img
     * @return
     */
    SeetaRect[] detect(byte[] img);
    SeetaRect[] detect(BufferedImage image);

    /**
     * 人脸识别(包含5个特征点位置)
     * @param image
     * @return
     */
    FaceLandmark detectLandmark(BufferedImage image);
    
    /**
     * 清除人脸库数据
     */
    void clear();    
    
```

- 示例代码：1:1人脸比对
```java
    @org.junit.Test
    public void testCompare() throws Exception {
        String img1 = "D:\\faces\\hg1.jpg";
        String img2 = "D:\\faces\\hg2.jpg";
        System.out.println("result:"+FaceHelper.compare(new File(img1), new File(img2)));
    }
```

- 示例代码：1:N人脸搜索
  先调用FaceHelper.register()方法将人脸图片注册到seetaface2的人脸库(内存)中 、目前人脸库最大支持存放500张，如果无法满足需求可以联系中科院进行商务合作
```java

    @org.junit.Test
    public void testSearch() throws IOException {
        SeetafaceBuilder.build();//系统启动时先调用初始化方法

        //等待初始化完成
        while (SeetafaceBuilder.getFaceDbStatus() == SeetafaceBuilder.FacedbStatus.LOADING || SeetafaceBuilder.getFaceDbStatus() == SeetafaceBuilder.FacedbStatus.READY) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //将人脸图片注册到人脸库中
        //将D:\faces目录下的jpg、png图片都注册到人脸库中
        Collection<File> files = FileUtils.listFiles(new File("D:\\faces"), new String[]{"jpg", "png"}, false);
        try {
            for (File file : files) {
                //注册时返回的图片在人脸库中的下标请自己存储 搜索时返回下标及相似度
                int index = FaceHelper.register(FileUtils.readFileToByteArray(file));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long l = System.currentTimeMillis();
        RecognizeResult result = FaceHelper.search(FileUtils.readFileToByteArray(new File("D:\\faces\\hg.jpg")));
        System.out.println("搜索结果：index=>" + result.index + " similar=>"+result.similar+"， 耗时：" + (System.currentTimeMillis() - l));
    }
```

c++源码：https://gitee.com/jamesluozhiwei/Seetafce2JNI-C