package com.orderapp.ordering.dto;

import java.util.List;

public record CustomerMenuViewModelDTO(
	LocationContextDTO context,
	List<MenuCategoryDTO> categories,
	List<MenuProductDTO> products
) {
	public record LocationContextDTO(
		String businessName,
		String businessAvatarText,
		String businessLogoDataUrl,
		String locationTitle,
		String locationSubtitle,
		String statusLabel,
		String statusVariant
	) {}

	public record MenuCategoryDTO(
		String id,
		String name,
		String icon
	) {}

	public record MenuProductDTO(
		String id,
		String name,
		String description,
		int priceCents,
		String imageUrl,
		String icon,
		String categoryId,
		List<ModifierGroupDTO> modifierGroups
	) {}

	public record ModifierGroupDTO(
		long id,
		String name,
		int minSelectable,
		Integer maxSelectable,
		boolean required,
		List<ModifierOptionDTO> options
	) {}

	public record ModifierOptionDTO(
		long id,
		String name,
		Integer priceCents,
		Integer priceDeltaCents
	) {}
}
