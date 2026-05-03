package coffee_backend_4j.service.impl;

import coffee_backend_4j.config.OssConfig;
import coffee_backend_4j.service.OssService;
import com.aliyun.oss.OSS;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
* @author LiARnn
* @version 1.0
*/
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {
    private final OssConfig ossConfig;
    private final OSS ossClient;
    @Override
    public String uploadFile(MultipartFile file) throws Exception {
        String originalFilename =file.getOriginalFilename();
        if (originalFilename == null||originalFilename.lastIndexOf(".")<= 0){
            throw new Exception();
        }
//        拿到后缀
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/");
        String dataTime = LocalDate.now().format(dateTimeFormatter);
        String newFilePath = ossConfig.getFolder() + dataTime  + filename;
        ossClient.putObject(ossConfig.getBucketName(), newFilePath, file.getInputStream());
//        返回访问路径
        return "https://" + ossConfig.getBucketName() + "." +
                ossConfig.getEndpoint().replace("https://", "") + "/" + newFilePath;
    }
}
