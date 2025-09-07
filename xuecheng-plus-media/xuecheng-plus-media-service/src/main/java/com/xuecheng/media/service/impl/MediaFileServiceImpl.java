package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpUtil.getMimeType;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
 @Service
 @Slf4j
public class MediaFileServiceImpl implements MediaFileService {


  @Autowired
 MediaFilesMapper mediaFilesMapper;

  @Autowired
 MinioClient minioClient;


  @Autowired
  MediaFileService currentProxy;
  @Autowired
  MediaProcessMapper mediaProcessMapper;


  @Value("${minio.bucket.files}")
  private String bucket_mediafiles;
  @Value("${minio.bucket.videofiles}")
  private String bucket_video;
 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
  
  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }
 //获取文件默认存储目录路径 年/月/日
 private String getDefaultFolderPath() {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-", "/")+"/";
  return folder;
 }
 //获取文件的md5
 private String getFileMd5(File file) {
  try (FileInputStream fileInputStream = new FileInputStream(file)) {
   String fileMd5 = DigestUtils.md5Hex(fileInputStream);
   return fileMd5;
  } catch (Exception e) {
   e.printStackTrace();
   return null;
  }
 }

 /*
 * 图片上传接口实现
 *
 * */
 @Override
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
  //获取文件名
  String FileName = uploadFileParamsDto.getFilename();
  //获取文件扩展名
  String extensionName = StringUtils.substringAfterLast(FileName, ".");
  String mimeType = getMineType(extensionName);

  //获取本地文件路径 拼接存储路径信息并存到数据库中 文件的存储路径-> 2022/09/01/xxxx.png
  String fileMd5 = getFileMd5(new File(localFilePath));
  String objectName = getDefaultFolderPath() + fileMd5 + extensionName;
  boolean result = addMediaFilesToMinIO(localFilePath,mimeType,bucket_mediafiles,objectName);
  if(!result){
   XueChengPlusException.cast("上传文件失败");
  }
  //保存文件信息到数据库
  MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
  if(mediaFiles==null){
   XueChengPlusException.cast("文件上传后保存信息失败");
  }
  //准备返回的对象
  UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
  BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);

  return uploadFileResultDto;

 }

 /**
  * 将文件上传到minio
  * @param localFilePath 文件本地路径
  * @param mimeType 媒体类型
  * @param bucket 桶
  * @param objectName 对象名
  * @return
  */
 public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName){
  try {
   UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
           .bucket(bucket)//桶
           .filename(localFilePath) //指定本地文件路径
           .object(objectName)//对象名 放在子目录下
           .contentType(mimeType)//设置媒体文件类型
           .build();
   //上传文件
   minioClient.uploadObject(uploadObjectArgs);
   log.debug("上传文件到minio成功,bucket:{},objectName:{},错误信息:{}",bucket,objectName);
   return true;
  } catch (Exception e) {
   e.printStackTrace();
   log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}",bucket,objectName,e.getMessage());
  }
  return false;
 }
/*
* 通过扩展名获取mimeType
*
* */
 private  String getMineType(String extension) {
       //通过扩展名得到媒体资源类型 mimeType
       //根据扩展名取出mimeType
     if(extension == null){
      extension = "";
     }
       ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
       //枚举类型是未知类型
       String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
       if (extensionMatch != null) {
        mimeType = extensionMatch.getMimeType();
       }
       return mimeType;
    }

 /**
  * @description 将文件信息添加到文件表
  * @param companyId  机构id
  * @param fileMd5  文件md5值
  * @param uploadFileParamsDto  上传文件的信息
  * @param bucket  桶
  * @param objectName 对象名称
  * @return com.xuecheng.media.model.po.MediaFiles
  * @author Mr.M
  * @date 2022/10/12 21:22
  */
 @Transactional
 @Override
 public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName){
  //将文件信息保存到数据库
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if(mediaFiles == null){
   mediaFiles = new MediaFiles();
   BeanUtils.copyProperties(uploadFileParamsDto,mediaFiles);
   //文件id
   mediaFiles.setId(fileMd5);
   //机构id
   mediaFiles.setCompanyId(companyId);
   //桶
   mediaFiles.setBucket(bucket);
   //file_path
   mediaFiles.setFilePath(objectName);
   //file_id
   mediaFiles.setFileId(fileMd5);
   //url
   mediaFiles.setUrl("/"+bucket+"/"+objectName);
   //上传时间
   mediaFiles.setCreateDate(LocalDateTime.now());
   //状态
   mediaFiles.setStatus("1");
   //审核状态
   mediaFiles.setAuditStatus("002003");
   //插入数据库
   int insert = mediaFilesMapper.insert(mediaFiles);
   if(insert<=0){
    log.debug("向数据库保存文件失败,bucket:{},objectName:{}",bucket,objectName);
    return null;
   }
   //记录待处理任务 视频上传成功后添加 TODO 对文件的mimeType进行判断 提取一个公共的方法 这里是对avi视频进行数据的插入
   addMediaTask(mediaFiles);

   return mediaFiles;

  }
  return mediaFiles;

 }

 /*
 * 添加待处理任务
 * @param MediaFiles mediaFiles 媒资文件信息
 * */
 private void addMediaTask(MediaFiles mediaFiles){
  //获取文件名称
  String fileName = mediaFiles.getFilename();
  //获取文件扩展名
  String extension= fileName.substring(fileName.lastIndexOf("."));
  String mimeType =getMineType(extension);
  // TODO 这里可以将多种mimeType进行判断 弄一个集合 判断当前的mimeType是否在集合中
  if(mimeType.equals("video/x-msvideo")){
    //如果是avi视频写入待处理任务
   MediaProcess mediaProcess = new MediaProcess();
   BeanUtils.copyProperties(mediaFiles,mediaProcess);
   //设置状态 TODO 不使用魔法值 采用枚举进行状态设置
   mediaProcess.setStatus("1");
   mediaProcess.setCreateDate(LocalDateTime.now());
   mediaProcess.setFailCount(0);
   //插入数据
   mediaProcessMapper.insert(mediaProcess);
  }
 }


 /*
 * 检查文件是否存在
 * */
 @Override
 public RestResponse<Boolean> checkFile(String fileMd5) {
  //先查询数据库
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if(mediaFiles!=null){
   //获取桶名称
   String Bucket = mediaFiles.getBucket();
   //objectName
   String filePath = mediaFiles.getFilePath();
   //如果数据库存在文件信息 查询minio
   GetObjectArgs getObjectArgs = GetObjectArgs.builder()
           .bucket(Bucket)
           .object(filePath)
           .build();

   try {
    //获取一个远程输入流
    FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
    if (inputStream!=null){
     //文件已经存在
     return RestResponse.success(true);
    }
   } catch (Exception e) {
     e.printStackTrace();
   }
  }
  //文件不存在
  return RestResponse.success(false);
 }

 /*
 *检查分块序号
 * */
 @Override
 public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

  //objectName
  String chunkFilePath = getChunkFileFolderPath(fileMd5);
  //查询minio
  GetObjectArgs getObjectArgs = GetObjectArgs.builder()
          .bucket(bucket_video)
          .object(chunkFilePath+chunkIndex)
          .build();

  try {
   //获取一个远程输入流
   FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
   if (inputStream!=null){
    //文件已经存在
    return RestResponse.success(true);
   }
  } catch (Exception e) {
   e.printStackTrace();
  }
  //文件不存在
  return RestResponse.success(false);
 }

 /*
 * 上传分块文件
 * */
 @Override
 public RestResponse uploadChunk(String fileMd5, int chunkIndex, String localChunkFilePath) {
  //获取分块文件的路径
  String chunkFilePath = getChunkFileFolderPath(fileMd5)+chunkIndex;
  //获取mineType 这里getMine是对空字符串进行了处理 将其转为了未知流
  String mimeType = getMineType(null);
  //上传文件到minio
  boolean b = addMediaFilesToMinIO(localChunkFilePath,mimeType,bucket_video,chunkFilePath);
  if(!b){
   return RestResponse.validfail(false,"上传分块文件失败");
  }
  return RestResponse.success(true);
 }

 /*
 * 合并分块文件
 * */
 @Override
 public RestResponse mergeChunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {

  //获取分块文件所在路径
  String chunkFilePath = getChunkFileFolderPath(fileMd5);
  //1.找到分块文件调用minio的SDK进行文件合并
  //采用流式写法
  List<ComposeSource> sourceList= Stream.iterate(0, i->++i).limit(chunkTotal).map(i->
                  ComposeSource.builder().
                          bucket(bucket_video).
                          object(chunkFilePath + i).
                          build()).
          collect(Collectors.toList ());
  //获取源文件名称
  String fileName = uploadFileParamsDto.getFilename();
  //从源文件中获取扩展名
  String extension =  fileName.substring(fileName.lastIndexOf("."));

  //获取合并后的文件路径即objectName
  String objectName = getFilePathByMd5(fileMd5,extension);


  //指定合并后的objectName等信息
  ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
          .bucket(bucket_video)
          //合并后的文件的目录
          .object(objectName)
          .sources(sourceList) //指定源文件信息
          .build();

  //合并文件
  try {
   minioClient.composeObject(composeObjectArgs);
  } catch (Exception e) {
   e.printStackTrace();
   log.info("合并文件失败,bucket:{},objectName:{},错误信息:{}",bucket_video,objectName,e.getMessage());
   return RestResponse.validfail(false,"合并文件失败");
  }
  //校验合并后的和源文件是否一致 一致则上传成功
  //先下载合并后的文件 TODO 方法优化
  File file = downloadFileFromMinIO(bucket_video,objectName);
  //计算合并后的MD5
  try(FileInputStream fileInputStream = new FileInputStream(file)){
    String mergeFileMd5 = DigestUtils.md5Hex(fileInputStream);
    if(!fileMd5.equals(mergeFileMd5)){
     log.error("合并文件校验失败,md5不一致,原始文件:{},合并文件{}",fileMd5,mergeFileMd5);
     return RestResponse.validfail(false,"合并文件校验失败");
    }
    //设置文件的大小
    uploadFileParamsDto.setFileSize(file.length());
  }catch (Exception e){

  }
  //将文件信息入库 (非事务方法调用一个事务方法 需要用一个代理对象 否则事务无法控制)
  MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId,fileMd5,uploadFileParamsDto,bucket_video,objectName);
  if(mediaFiles == null){
   log.debug("上传文件入库失败,bucket:{},objectName:{}",bucket_video,objectName);
   return RestResponse.validfail(false,"上传文件入库失败");
  }
  //清理原本的分块文件
  clearChunkFiles(chunkFilePath,chunkTotal);

  return RestResponse.success(true);
 }


 /**
  * 从minio下载文件
  * @param bucket 桶
  * @param objectName 对象名称
  * @return 下载后的文件
  */
 public File downloadFileFromMinIO(String bucket,String objectName){
  //临时文件
  File minioFile = null;
  FileOutputStream outputStream = null;
  try{
   InputStream stream = minioClient.getObject(GetObjectArgs.builder()
           .bucket(bucket)
           .object(objectName)
           .build());
   //创建临时文件
   minioFile=File.createTempFile("minio", ".merge");
   outputStream = new FileOutputStream(minioFile);
   IOUtils.copy(stream,outputStream);
   return minioFile;
  } catch (Exception e) {
   e.printStackTrace();
  }finally {
   if(outputStream!=null){
    try {
     outputStream.close();
    } catch (IOException e) {
     e.printStackTrace();
    }
   }
  }
  return null;
 }
 /**
  * 清除分块文件
  * @param chunkFileFolderPath 分块文件路径
  * @param chunkTotal 分块文件总数
  */
 private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal){

   List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
           .limit(chunkTotal)
           .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
           .collect(Collectors.toList());

   RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
            .bucket(bucket_video)
            .objects(deleteObjects)
            .build();

   Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
  results.forEach(f->{
   try {
    DeleteError deleteError = f.get();
   } catch (Exception e) {
    e.printStackTrace();
   }
  });

 }

 //获取分块文件的目录
 private String getChunkFileFolderPath(String fileMd5) {
  return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" + "chunks" + "/";
 }
 /*
  * 得到合并后的文件的地址
  * @param fileMd5 文件id即md5值
  * @param fileExt 文件扩展名
  * @return
  * */
 public   String getFilePathByMd5(String fileMd5,String fileExt) {
  return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/"+fileMd5 + fileExt;
 }

 @Override
 public MediaFiles getFileById(String mediaId) {
  MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
  return mediaFiles;
 }
}
