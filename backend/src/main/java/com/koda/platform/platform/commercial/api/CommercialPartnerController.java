package com.koda.platform.platform.commercial.api;

import com.koda.platform.platform.commercial.application.CommercialPartner;
import com.koda.platform.platform.commercial.application.CommercialPartnerService;
import com.koda.platform.platform.commercial.application.CommercialRequestMetadata;
import com.koda.platform.platform.commercial.application.CreateCommercialPartnerCommand;
import com.koda.platform.platform.commercial.application.UpdateCommercialPartnerCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CommercialPartnerController {

    private final CommercialPartnerService service;

    public CommercialPartnerController(CommercialPartnerService service) {
        this.service = service;
    }

    @GetMapping("/customers")
    public List<CommercialPartnerResponse> listCustomers(@RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return service.listCustomers(limit).stream().map(CommercialPartnerResponse::from).toList();
    }

    @GetMapping("/customers/{id}")
    public CommercialPartnerResponse getCustomer(@PathVariable UUID id) {
        return CommercialPartnerResponse.from(service.getCustomer(id));
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public CommercialPartnerResponse createCustomer(@Valid @RequestBody CommercialPartnerRequest request, HttpServletRequest httpRequest) {
        return CommercialPartnerResponse.from(service.createCustomer(request.toCreateCommand(), metadata(httpRequest)));
    }

    @PutMapping("/customers/{id}")
    public CommercialPartnerResponse updateCustomer(@PathVariable UUID id, @Valid @RequestBody VersionedCommercialPartnerRequest request,
                                                    HttpServletRequest httpRequest) {
        return CommercialPartnerResponse.from(service.updateCustomer(id, request.toUpdateCommand(), metadata(httpRequest)));
    }

    @DeleteMapping("/customers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable UUID id, @RequestParam @Min(0) long version, HttpServletRequest httpRequest) {
        service.deleteCustomer(id, version, metadata(httpRequest));
    }

    @GetMapping("/suppliers")
    public List<CommercialPartnerResponse> listSuppliers(@RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return service.listSuppliers(limit).stream().map(CommercialPartnerResponse::from).toList();
    }

    @GetMapping("/suppliers/{id}")
    public CommercialPartnerResponse getSupplier(@PathVariable UUID id) {
        return CommercialPartnerResponse.from(service.getSupplier(id));
    }

    @PostMapping("/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    public CommercialPartnerResponse createSupplier(@Valid @RequestBody CommercialPartnerRequest request, HttpServletRequest httpRequest) {
        return CommercialPartnerResponse.from(service.createSupplier(request.toCreateCommand(), metadata(httpRequest)));
    }

    @PutMapping("/suppliers/{id}")
    public CommercialPartnerResponse updateSupplier(@PathVariable UUID id, @Valid @RequestBody VersionedCommercialPartnerRequest request,
                                                    HttpServletRequest httpRequest) {
        return CommercialPartnerResponse.from(service.updateSupplier(id, request.toUpdateCommand(), metadata(httpRequest)));
    }

    @DeleteMapping("/suppliers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSupplier(@PathVariable UUID id, @RequestParam @Min(0) long version, HttpServletRequest httpRequest) {
        service.deleteSupplier(id, version, metadata(httpRequest));
    }

    private CommercialRequestMetadata metadata(HttpServletRequest request) {
        return new CommercialRequestMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public record CommercialPartnerRequest(
        @NotBlank @Size(max = 220) String legalName,
        @Size(max = 220) String commercialName,
        @Size(max = 40) String documentType,
        @Size(max = 80) String documentNumber,
        @Size(max = 80) String taxCondition,
        @Email @Size(max = 254) String email,
        @Size(max = 80) String phone,
        @Size(max = 240) String addressLine,
        @Size(max = 120) String city,
        @Size(max = 80) String provinceCode,
        @Size(max = 2) String countryCode,
        @Size(max = 1000) String notes,
        @Size(max = 32) String status
    ) {
        CreateCommercialPartnerCommand toCreateCommand() {
            return new CreateCommercialPartnerCommand(legalName, commercialName, documentType, documentNumber, taxCondition, email, phone,
                addressLine, city, provinceCode, countryCode, notes, status);
        }
    }

    public record VersionedCommercialPartnerRequest(
        @Min(0) long version,
        @NotBlank @Size(max = 220) String legalName,
        @Size(max = 220) String commercialName,
        @Size(max = 40) String documentType,
        @Size(max = 80) String documentNumber,
        @Size(max = 80) String taxCondition,
        @Email @Size(max = 254) String email,
        @Size(max = 80) String phone,
        @Size(max = 240) String addressLine,
        @Size(max = 120) String city,
        @Size(max = 80) String provinceCode,
        @Size(max = 2) String countryCode,
        @Size(max = 1000) String notes,
        @NotNull @Size(max = 32) String status
    ) {
        UpdateCommercialPartnerCommand toUpdateCommand() {
            return new UpdateCommercialPartnerCommand(version, legalName, commercialName, documentType, documentNumber, taxCondition, email, phone,
                addressLine, city, provinceCode, countryCode, notes, status);
        }
    }

    public record CommercialPartnerResponse(
        String id,
        String roleType,
        String legalName,
        String commercialName,
        String documentType,
        String documentNumber,
        String taxCondition,
        String email,
        String phone,
        String addressLine,
        String city,
        String provinceCode,
        String countryCode,
        String notes,
        String status,
        boolean system,
        long version,
        Instant updatedAt
    ) {
        static CommercialPartnerResponse from(CommercialPartner partner) {
            return new CommercialPartnerResponse(
                partner.id().toString(),
                partner.roleType(),
                partner.legalName(),
                partner.commercialName(),
                partner.documentType(),
                partner.documentNumber(),
                partner.taxCondition(),
                partner.email(),
                partner.phone(),
                partner.addressLine(),
                partner.city(),
                partner.provinceCode(),
                partner.countryCode(),
                partner.notes(),
                partner.status(),
                partner.system(),
                partner.version(),
                partner.updatedAt()
            );
        }
    }
}