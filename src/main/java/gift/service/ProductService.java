package gift.service;

import gift.dto.ProductDTO;
import gift.dto.WishDTO;
import gift.entity.Product;
import gift.exception.BadRequestExceptions.BadRequestException;
import gift.exception.BadRequestExceptions.InvalidIdException;
import gift.exception.BadRequestExceptions.NoSuchProductIdException;
import gift.exception.InternalServerExceptions.InternalServerException;
import gift.repository.ProductRepository;
import gift.repository.WishRepository;
import gift.util.validator.databaseValidator.ProductDatabaseValidator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final WishRepository wishRepository;

    @Autowired
    public ProductService (ProductRepository productRepository, WishRepository wishRepository) {
        this.productRepository = productRepository;
        this.wishRepository = wishRepository;
    }

    @Transactional
    public void addProduct(ProductDTO productDTO) throws RuntimeException {
        try {
            Product product = ProductDTO.convertToProduct(productDTO);
            productRepository.save(product);
        } catch (Exception e) {
            if (e instanceof DataIntegrityViolationException) {
                throw new BadRequestException("잘못된 제품 값을 입력했습니다. 입력 칸 옆의 설명을 다시 확인해주세요");
            }
            if (!(e instanceof BadRequestException)) {
                throw new InternalServerException(e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductList(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);

        List<ProductDTO> productDTOList = productPage.getContent().stream().map(ProductDTO::convertToProductDTO).toList();

        return new PageImpl<>(productDTOList, productPage.getPageable(), productPage.getTotalElements());
    }

    @Transactional
    public void updateProduct(Long id, ProductDTO productDTO) throws RuntimeException {
        if(!id.equals(productDTO.getId()))
            throw new InvalidIdException("올바르지 않은 id입니다.");

        Optional<Product> productInDb = productRepository.findById(id);

        if (productInDb.isEmpty())
            throw new NoSuchProductIdException("id가 %d인 상품은 존재하지 않습니다.".formatted(id));

        Product product = ProductDTO.convertToProduct(productDTO);
        Product productInDB = productInDb.get();
        productInDB.changeProduct(product.getName(), product.getPrice(), product.getImageUrl());
    }

    @Transactional
    public void deleteProduct(Long id) throws RuntimeException {
        try {
            wishRepository.deleteByProductId(id); // 외래키 제약조건
            productRepository.deleteById(id);
        } catch (Exception e) {
            if (e instanceof EmptyResultDataAccessException) {
                throw new NoSuchProductIdException("id가 %d인 상품은 존재하지 않습니다.".formatted(id));
            }
            throw new InternalServerException(e.getMessage());
        }
    }
}


