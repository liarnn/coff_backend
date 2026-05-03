package coffee_backend_4j.controller;

import coffee_backend_4j.dto.AddressDTO;
import coffee_backend_4j.service.AddressService;
import coffee_backend_4j.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public Result<AddressDTO> createAddress(@RequestBody AddressDTO addressDTO) {
        return Result.ok(addressService.createAddress(addressDTO));
    }

    @PutMapping
    public Result<Void> updateAddress(@RequestBody AddressDTO addressDTO) {
        addressService.updateAddress(addressDTO);
        return Result.ok();
    }

    @GetMapping
    public Result<Result.PageData<List<AddressDTO>>> getAddressList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(addressService.getAddressList(page, size));
    }

    @GetMapping("/{addressId}")
    public Result<AddressDTO> getAddress(@PathVariable Integer addressId) {
        return Result.ok(addressService.getAddress(addressId));
    }

    @DeleteMapping
    public Result<Void> deleteAddress(@RequestBody Map<String, Object> request) {
        Integer addressId = (Integer) request.get("address_id");
        addressService.deleteAddress(addressId);
        return Result.ok();
    }
}
