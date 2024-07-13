package gift.service;

import gift.dto.MemberDTO;
import gift.dto.ProductDTO;
import gift.dto.WishListDTO;
import gift.entity.Member;
import gift.entity.Product;
import gift.entity.Wish;
import gift.exception.BadRequestExceptions.BadRequestException;
import gift.exception.BadRequestExceptions.NoSuchProductIdException;
import gift.exception.BadRequestExceptions.UserNotFoundException;
import gift.repository.MemberRepository;
import gift.repository.WishRepository;
import gift.util.converter.MemberConverter;
import gift.util.converter.WishListConverter;
import gift.util.validator.ParameterValidator;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishListService {

    private final MemberRepository memberRepository;
    private final WishRepository wishRepository;
    private final ParameterValidator parameterValidator;

    @Autowired
    public WishListService(MemberRepository memberRepository,
            WishRepository wishRepository, ParameterValidator parameterValidator) {
        this.memberRepository = memberRepository;
        this.wishRepository = wishRepository;
        this.parameterValidator = parameterValidator;
    }

    @Transactional(readOnly = true)
    public WishListDTO getWishList(MemberDTO memberDTO)
            throws RuntimeException {

        Optional<Member> optionalMember =
                memberRepository.findByEmail(MemberConverter.convertToMember(memberDTO).getEmail());

        if (optionalMember.isEmpty()) {
            throw new UserNotFoundException(memberDTO.getEmail() + "을(를) 가지는 유저를 찾을 수 없습니다.");
        }
        Member member = optionalMember.get();
        return WishListConverter.convertToWishListDTO(member.getWishList());
    }

    @Transactional
    public void addWishes(MemberDTO memberDTO, ProductDTO productDTO) {
        Map<String, Object> validatedParameterMap = parameterValidator.validateParameter(memberDTO,
                productDTO);
        Member member = (Member) validatedParameterMap.get("member");
        Product product = (Product) validatedParameterMap.get("product");

        Optional<Wish> optionalWish = wishRepository.findByMemberAndProduct(member, product);
        if (optionalWish.isEmpty()) {
            Wish wish = new Wish(member, product, 1);
            wishRepository.save(wish);
            return;
        }
        Wish existingWish = optionalWish.get();
        existingWish.incrementQuantity();
    }

    @Transactional
    public void removeWishListProduct(MemberDTO memberDTO, Long id)
            throws NoSuchProductIdException, EmptyResultDataAccessException {
        try {
            Map<String, Object> validatedParameterMap = parameterValidator.validateParameter(
                    memberDTO, id);
            Member member = (Member) validatedParameterMap.get("member");
            Product product = (Product) validatedParameterMap.get("product");
            wishRepository.deleteByMemberAndProductId(member, product.getId());
        } catch (NoSuchProductIdException e) { //제품 목록에는 없는데 유저는 존재하는 경우
            wishRepository.deleteByMemberAndProductId(parameterValidator.validateMember(memberDTO), id);
        }
    }

    @Transactional
    public void setWishListNumber(MemberDTO memberDTO, ProductDTO productDTO, Integer quantity)
            throws RuntimeException {
        Map<String, Object> validatedParameterMap = parameterValidator.validateParameter(memberDTO,
                productDTO);
        Member member = (Member) validatedParameterMap.get("member");
        Product product = (Product) validatedParameterMap.get("product");
        Optional<Wish> optionalWish = wishRepository.findByMemberAndProduct(member, product);

        if (optionalWish.isEmpty()) {
            throw new BadRequestException("위시리스트에 그러한 품목을 찾을 수 없습니다.");
        }
        Wish wish = optionalWish.get();
        wish.changeQuantity(quantity);
    }

}
