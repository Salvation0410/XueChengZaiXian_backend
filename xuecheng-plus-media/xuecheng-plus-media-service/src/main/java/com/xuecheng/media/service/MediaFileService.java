package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.File;
import java.util.List;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService {

 /**
  * @description 媒资文件查询方法
  * @param pageParams 分页参数
  * @param queryMediaParamsDto 查询条件
  * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
  * @author Mr.M
  * @date 2022/9/10 8:57
 */
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

 /**
  * @description 上传文件接口
  * @param companyId 机构id
  * @param uploadFileParamsDto 上传文件的信息
  * @param localFilePath 文件在服务器中的存储路径
  * @return com.xuecheng.media.model.dto.UploadFileResultDto
  * @author huang
  * @date 2025 /8/19
 */
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto,String localFilePath);

 /**
  * @description 将文件信息添加到数据库
  * @param companyId 机构id
  * @param fileMd5 文件md5值
  * @param uploadFileParamsDto 文件信息
  * @param bucket 文件所属的桶
  * @param objectName 文件在桶中的名称
  * @return com.xuecheng.media.model.po.MediaFiles
  * @author huang
  * @date 2025 /8/19
 */
 public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName);

 /**
  * @description 检查文件是否存在
  * @param fileMd5 文件的md5
  * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
  * @author Mr.M
  * @date 2022/9/13 15:38
  */
 public RestResponse<Boolean> checkFile(String fileMd5);
 /**
  * @description 检查分块文件
  * @param fileMd5 文件md5值
  * @param chunkIndex 分块序号
  * @return boolean
  * @author huang
  * @date 2025 /8/19
 */
 public RestResponse<Boolean> checkChunk(String fileMd5,int chunkIndex);

 /**
  * @description 上传分块文件
  * @param localChunkFilePath 分块文件的路径
  * @param fileMd5 文件md5值
  * @param chunkIndex 分块序号
  * @return com.xuecheng.base.model.RestResponse
  * @author huang
  * @date 2025 /8/19
 */
 public RestResponse uploadChunk(String fileMd5,int chunkIndex,String localChunkFilePath);

 /**
  * @description 合并分块
  * @param companyId  机构id
  * @param fileMd5  文件md5
  * @param chunkTotal 分块总和
  * @param uploadFileParamsDto 文件信息
  * @return com.xuecheng.base.model.RestResponse
  * @author Mr.M/
  * @date 2025 /8/19
  */
 public RestResponse mergeChunks(Long companyId,String fileMd5,int chunkTotal,UploadFileParamsDto uploadFileParamsDto);
 /**
  * 从minio下载文件
  * @param bucket 桶
  * @param objectName 对象名称
  * @return 下载后的文件
  */
 public File downloadFileFromMinIO(String bucket, String objectName);
 /**
  * 将文件上传到minio
  * @param localFilePath 文件本地路径
  * @param mimeType 媒体类型
  * @param bucket 桶
  * @param objectName 对象名
  * @return
  */
 public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName);
 /*
  * 得到合并后的文件的地址
  * @param fileMd5 文件id即md5值
  * @param fileExt 文件扩展名
  * @return
  * */
 public String getFilePathByMd5(String fileMd5,String fileExt);

 /*
 * 根据媒资文件id查询相应课程计划
 *
 * */
 MediaFiles getFileById(String mediaId);
}
