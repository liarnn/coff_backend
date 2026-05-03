package coffee_backend_4j.service;

import org.springframework.web.multipart.MultipartFile;

/**
* @author LiARnn
* @version 1.0
*/

public interface OssService {
    String uploadFile(MultipartFile  file) throws Exception;
}
