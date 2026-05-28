package ar.com.rotula.service;

import ar.com.rotula.domain.Product;
import ar.com.rotula.dto.PageResponse;
import ar.com.rotula.dto.ProductRequest;
import ar.com.rotula.dto.ProductResponse;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(int page, int size, String sortParam) {
        UUID tenantId = currentUser().getTenantId();
        Sort sort = parseSort(sortParam);
        Pageable pageable = PageRequest.of(page, size, sort);
        return PageResponse.from(
                productRepository.findByTenantId(tenantId, pageable),
                ProductResponse::from
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return ProductResponse.from(requireOwned(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        AppUserDetails user = currentUser();
        Product product = Product.builder()
                .tenantId(user.getTenantId())
                .createdBy(user.getId())
                .name(req.name())
                .category(req.category())
                .netWeight(req.netWeight())
                .weightUnit(req.weightUnit())
                .rneNumber(req.rneNumber())
                .rnpaNumber(req.rnpaNumber())
                .servingSizeG(req.servingSizeG())
                .crossContamination(req.crossContamination())
                .status("draft")
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest req) {
        Product product = requireOwned(id);
        product.setName(req.name());
        product.setCategory(req.category());
        product.setNetWeight(req.netWeight());
        product.setWeightUnit(req.weightUnit());
        product.setRneNumber(req.rneNumber());
        product.setRnpaNumber(req.rnpaNumber());
        product.setServingSizeG(req.servingSizeG());
        product.setCrossContamination(req.crossContamination());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID id) {
        Product product = requireOwned(id);
        productRepository.delete(product);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Product requireOwned(UUID id) {
        UUID tenantId = currentUser().getTenantId();
        return productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado: " + id));
    }

    private AppUserDetails currentUser() {
        return (AppUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    /**
     * Parses "field,direction" strings such as "name,asc" or "createdAt,desc".
     * Falls back to createdAt DESC when the input is null/blank/invalid.
     */
    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sortParam.split(",", 2);
        String field = parts[0].trim();
        Sort.Direction direction = parts.length == 2
                && parts[1].trim().equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
