package storagecraft.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

public class SoldererRecipeCategory implements IRecipeCategory
{
	private IDrawable background;

	public SoldererRecipeCategory(IGuiHelper helper)
	{
		background = helper.createDrawable(new ResourceLocation("storagecraft", "textures/gui/solderer.png"), 0, 0, 176, 177);
	}

	@Override
	public String getUid()
	{
		return "storagecraft.solderer";
	}

	@Override
	public String getTitle()
	{
		return StatCollector.translateToLocal("gui.storagecraft:solderer");
	}

	@Override
	public IDrawable getBackground()
	{
		return background;
	}

	@Override
	public void drawExtras(Minecraft minecraft)
	{
	}

	@Override
	public void drawAnimations(Minecraft minecraft)
	{
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, IRecipeWrapper recipeWrapper)
	{
		IGuiItemStackGroup group = recipeLayout.getItemStacks();

		int x = 44;
		int y = 20;

		for (int i = 0; i < 3; ++i)
		{
			group.init(i, true, x, y);

			y += 18;
		}

		group.init(3, false, 134, 38);

		if (recipeWrapper instanceof SoldererRecipeWrapper)
		{
			for (int i = 0; i < 3; ++i)
			{
				group.set(i, (ItemStack) recipeWrapper.getInputs().get(i));
			}

			group.set(3, (ItemStack) recipeWrapper.getOutputs().get(0));
		}
	}
}