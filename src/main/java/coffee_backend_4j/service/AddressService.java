package coffee_backend_4j.service;

import coffee_backend_4j.dto.AddressDTO;
import coffee_backend_4j.utils.Result;

import java.util.List;

public interface AddressService {

    AddressDTO createAddress(AddressDTO addressDTO);

    void updateAddress(AddressDTO addressDTO);

    Result.PageData<List<AddressDTO>> getAddressList(Integer page, Integer size);

    void deleteAddress(Integer addressId);

    AddressDTO getAddress(Integer addressId);
}
