package coffee_backend_4j.service.impl;

import coffee_backend_4j.dto.AddressDTO;
import coffee_backend_4j.entity.Address;
import coffee_backend_4j.exception.BusinessException;
import coffee_backend_4j.mapper.AddressMapper;
import coffee_backend_4j.service.AddressService;
import coffee_backend_4j.utils.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public AddressDTO createAddress(AddressDTO addressDTO) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        addressDTO.setUserId(userId);

        if (Boolean.TRUE.equals(addressDTO.getIsDefault())) {
            Address oldDefaultAddress = addressMapper.selectOne(
                new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .eq(Address::getIsDefault, true)
            );
            if (oldDefaultAddress != null) {
                oldDefaultAddress.setIsDefault(false);
                oldDefaultAddress.setUpdatedAt(LocalDateTime.now());
                addressMapper.updateById(oldDefaultAddress);
            }
        }

        Address address = new Address();
        address.setUserId(userId);
        address.setReceiver(addressDTO.getReceiver());
        address.setPhone(addressDTO.getPhone());
        address.setProvince(addressDTO.getProvince());
        address.setCity(addressDTO.getCity());
        address.setDistrict(addressDTO.getDistrict());
        address.setDetailAddress(addressDTO.getDetailAddress());
        address.setIsDefault(addressDTO.getIsDefault() != null ? addressDTO.getIsDefault() : false);
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());

        try {
            addressMapper.insert(address);
            return convertToDTO(address);
        } catch (Exception e) {
            System.out.println("创建地址失败：" + e.getMessage());
            throw new BusinessException("创建地址失败，请重试");
        }
    }

    @Override
    @Transactional
    public void updateAddress(AddressDTO addressDTO) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        Address dbAddress = addressMapper.selectById(addressDTO.getId());
        if (dbAddress == null) {
            throw new BusinessException("该地址不存在");
        }

        if (Boolean.TRUE.equals(addressDTO.getIsDefault())) {
            Address oldDefaultAddress = addressMapper.selectOne(
                new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .eq(Address::getIsDefault, true)
            );
            if (oldDefaultAddress != null && !oldDefaultAddress.getId().equals(addressDTO.getId())) {
                oldDefaultAddress.setIsDefault(false);
                oldDefaultAddress.setUpdatedAt(LocalDateTime.now());
                addressMapper.updateById(oldDefaultAddress);
            }
        }

        if (addressDTO.getReceiver() != null) dbAddress.setReceiver(addressDTO.getReceiver());
        if (addressDTO.getPhone() != null) dbAddress.setPhone(addressDTO.getPhone());
        if (addressDTO.getProvince() != null) dbAddress.setProvince(addressDTO.getProvince());
        if (addressDTO.getCity() != null) dbAddress.setCity(addressDTO.getCity());
        if (addressDTO.getDistrict() != null) dbAddress.setDistrict(addressDTO.getDistrict());
        if (addressDTO.getDetailAddress() != null) dbAddress.setDetailAddress(addressDTO.getDetailAddress());
        if (addressDTO.getIsDefault() != null) dbAddress.setIsDefault(addressDTO.getIsDefault());
        dbAddress.setUpdatedAt(LocalDateTime.now());

        try {
            addressMapper.updateById(dbAddress);
            return;
        } catch (Exception e) {
            System.out.println("更新地址失败：" + e.getMessage());
            throw new BusinessException("更新地址失败，请重试");
        }
    }

    @Override
    public coffee_backend_4j.utils.Result.PageData<List<AddressDTO>> getAddressList(Integer page, Integer size) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        List<Address> addresses = addressMapper.selectList(
            new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId)
        );
        List<AddressDTO> addressDTOs = addresses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        coffee_backend_4j.utils.Result.PageData<List<AddressDTO>> pageData =
                new coffee_backend_4j.utils.Result.PageData<>(addressDTOs, page, size, (long) addresses.size());
        return pageData;
    }

    @Override
    @Transactional
    public void deleteAddress(Integer addressId) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("该地址不存在");
        }

        if (!isAdmin(userId) && !address.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该地址");
        }

        addressMapper.deleteById(addressId);
        return;
    }

    @Override
    public AddressDTO getAddress(Integer addressId) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }

        if (!isAdmin(userId) && !address.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该地址");
        }

        return convertToDTO(address);
    }

    private boolean isAdmin(Integer userId) {
        return userId != null && userId <= 100;
    }

    private AddressDTO convertToDTO(Address address) {
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setUserId(address.getUserId());
        dto.setReceiver(address.getReceiver());
        dto.setPhone(address.getPhone());
        dto.setProvince(address.getProvince());
        dto.setCity(address.getCity());
        dto.setDistrict(address.getDistrict());
        dto.setDetailAddress(address.getDetailAddress());
        dto.setIsDefault(address.getIsDefault());
        return dto;
    }
}
