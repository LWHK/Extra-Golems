package com.mcmoddev.golems.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mcmoddev.golems.main.ExtraGolems;
import com.mcmoddev.golems.main.GolemItems;
import com.mcmoddev.golems.util.config.GolemContainer;
import com.mcmoddev.golems.util.config.GolemRegistrar;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class GuiGolemBook extends Screen {

  //////////// GUI GOLEM BOOK LAYOUT ////////////
  // [X] 2-Page Layout:
  ////// [X] Each screen will have a double-version of the
  ////// default Minecraft Book GUI.
  ////// [X] There will be arrows
  ////// for "Previous Page" and "Next Page" but each one
  ////// will actually change the page count by (2) because
  ////// we show 2 pages at a time.
  // [ ] Buttons:
  ////// [X] Like Minecraft book, there is a "Done" button below.
  ////// [X] Also the Previous/Next Page buttons as described above.
  ////// [ ] Button to jump to page 1
  // [X] PAGE 1 (Left): WELCOME
  ////// [X] Small blurb about golems
  // [X] PAGE 2 (Right): TABLE OF CONTENTS
  ////// [X] Lists the name of each Golem in the book.
  ////// [X] Click-able (linked) list of all Golem names, maybe
  ////// with an icon of their building block.
  ////// [X] Clicking that name will jump you to the correct page.
  // [X] PAGE 3 (Left): GOLEM SPELL RECIPE
  ////// [X] Name of item + Picture of crafting grid.
  // [X] PAGE 4 (Right): GOLEM HEAD RECIPE
  ////// [X] Name of block + Picture of crafting grid.
  // [X] PAGE 5 (Left): HOW TO BUILD A GOLEM
  ////// Verbal instructions on where to place blocks, etc.
  // [X] PAGE 6 (Right): IMAGE OF HOW TO BUILD A GOLEM
  ////// [X] Diagram of 4 blocks in golem-shape and a Golem Head
  ////// above them, indicating where to place the head.
  ////// [X] Extra Fancy option: blocks are 3d
  // [X] PAGES 7+ (Left and Right): GOLEM ENTRIES
  ////// Defined in the GolemBookEntry object:
  ////// [X] Large picture of Building Block on upper-left of page.
  ////// [X] Hover-over text tells you the name of the Block.
  ////// [X] Page number in upper-right: "Page x of xx"
  ////// [X] Golem Name below page number, right-aligned
  ////// [X] Attributes listed:
  ////// Health and Attack
  ////// "Multi-Textured" prompt if Golem has multiple textures
  ////// "Fireproof" if the Golem is fireproof
  ////// All Golem Specials as defined in GolemBase#addSpecialDesc(List<String>)
  ////// (each entry as a separate line)

  ///////////////////////////////////////////////

  // The texture used by the majority of this gui
  protected static final ResourceLocation TEXTURE = new ResourceLocation(ExtraGolems.MODID, "textures/gui/info_book.png");
  protected static final ResourceLocation CONTENTS = new ResourceLocation(ExtraGolems.MODID, "textures/gui/info_book_contents.png");

  // book texture has these dimensions
  protected static final int BOOK_HEIGHT = 164;
  protected static final int BOOK_WIDTH = 256;
  // icons are this many pixels apart in the textures
  protected static final int DEF_SEP = 5;

  // how far down the Minecraft screen the book starts
  protected static final int SCR_OFFSET_Y = 16;
  // width of the arrow button and its texture
  protected static final int ARROW_WIDTH = 13 + DEF_SEP;
  protected static final int ARROW_HEIGHT = 10 + DEF_SEP;
  /**
   * size of the supplemental (optional) image for each golem. As long as it's a
   * 2:1 ratio, it'll work
   **/
  protected static final int SUPP_WIDTH = 100;
  protected static final int SUPP_HEIGHT = 50;

  // If a Block is displayed on the LEFT side, this helps with the tooltip
  private GuiGolemBook.BlockButton buttonBlockLeft;
  // If a Block is displayed on the RIGHT side, this helps with the tooltip
  private GuiGolemBook.BlockButton buttonBlockRight;
  // Increments the page number by 2
  private GuiGolemBook.NextPageButton buttonNextPage;
  // Decrements the page number by 2
  private GuiGolemBook.NextPageButton buttonPreviousPage;
  // Clickable link to each golem, used on page 2
  private GuiGolemBook.GolemEntryButton[] tableOfContents;

  protected int curPage;
  protected int totalPages;

  private static final List<GolemBookEntry> GOLEMS = new ArrayList<>();
  private static final List<GolemBookEntry> ALPHABETICAL = new ArrayList<>();

  private static final float GOLEM_BLOCK_SCALE = 1.60F;
  private static final int MARGIN = 12;
  private static final int NUM_PAGES_INTRO = 6;
  // there are six entries showing at a time
  private static final int NUM_CONTENTS_ENTRIES = 5;

  protected static final int CONTENTS_W = 106;
  protected static final int CONTENTS_H = 110;
  protected static final int SCROLL_STARTX = MARGIN;
  protected static final int SCROLL_STARTY = MARGIN * 2;
  protected static final int SCROLL_W = 12;
  protected static final int SCROLL_H = 15;
  protected static final int ENTRY_W = 88;
  protected static final int ENTRY_H = 22;
  /**
   * Amount scrolled in Table of Contents (0 = top, 1 = bottom)
   */
  private float currentScroll;
  /**
   * True if the scrollbar is being dragged
   */
  private boolean isScrolling;
  /**
   * in-game tick counter for when GUI is paused
   **/
  protected long ticksOpen;

  // for use in drawing golem spell recipe
  private static final ItemStack[] ingredientsSpell = new ItemStack[] { new ItemStack(Items.PAPER), new ItemStack(Items.FEATHER),
      new ItemStack(Items.REDSTONE), new ItemStack(Items.INK_SAC) };
  private static final ItemStack outputSpell = new ItemStack(GolemItems.GOLEM_SPELL);
  // for use in drawing golem head recipe
  private static final ItemStack[] ingredientsHead = new ItemStack[] { new ItemStack(Blocks.CARVED_PUMPKIN), new ItemStack(GolemItems.GOLEM_SPELL) };
  private static final ItemStack outputHead = new ItemStack(GolemItems.GOLEM_HEAD);

  public GuiGolemBook(PlayerEntity playerIn, ItemStack itemIn) {
    super(new TranslationTextComponent("item.golems.info_book"));
    if (GOLEMS.isEmpty()) {
      initGolemBookEntries();
    }
    this.setSize(BOOK_WIDTH, BOOK_HEIGHT);
    this.curPage = 0;
    this.totalPages = NUM_PAGES_INTRO + GOLEMS.size();
    this.currentScroll = 0;
    this.isScrolling = false;
    this.tableOfContents = new GuiGolemBook.GolemEntryButton[NUM_CONTENTS_ENTRIES];
    this.ticksOpen = 0L;
  }

  /**
   * Populates the GolemEntry list to use in book gui
   **/
  private static final void initGolemBookEntries() {
    GOLEMS.clear();
    for (GolemContainer container : GolemRegistrar.getContainers()) {
      if (container.isEnabled()) {
        GOLEMS.add(new GolemBookEntry(container));
      }
    }
    // sort golems by attack power
    Collections.sort(GOLEMS, (g1, g2) -> Float.compare(g1.getAttack(), g2.getAttack()));

    // make and sort alphabetical list
    ALPHABETICAL.clear();
    ALPHABETICAL.addAll(GOLEMS);
    Collections.sort(ALPHABETICAL, (g1, g2) -> g1.getGolemName().compareTo(g2.getGolemName()));
  }

  @Override
  public void init() {
    // add the "close gui" button
    int doneW = 98;
    int doneH = 20;
    int doneX = (this.width - doneW) / 2;
    int doneY = BOOK_HEIGHT + SCR_OFFSET_Y + 8;
    this.addButton(new Button(doneX, doneY, doneW, doneH, I18n.format("gui.done"), c -> onClose()));
    // locate and activate the "page change" arrows
    int arrowX = (this.width - BOOK_WIDTH) / 2;
    int arrowY = SCR_OFFSET_Y + BOOK_HEIGHT - (ARROW_HEIGHT * 3 / 2);
    this.buttonPreviousPage = this.addButton(new GuiGolemBook.NextPageButton(this, arrowX + ARROW_WIDTH, arrowY, false));
    this.buttonNextPage = this.addButton(new GuiGolemBook.NextPageButton(this, arrowX + BOOK_WIDTH - ARROW_WIDTH * 2, arrowY, true));
    // calculate location and size of blocks icons, if present
    int blockX = ((this.width - BOOK_WIDTH) / 2) + MARGIN + 4;
    int blockY = SCR_OFFSET_Y + MARGIN;
    this.buttonBlockLeft = this.addButton(new GuiGolemBook.BlockButton(this, new Block[] {}, blockX, blockY, GOLEM_BLOCK_SCALE));
    blockX = (this.width / 2) + MARGIN;
    this.buttonBlockRight = this.addButton(new GuiGolemBook.BlockButton(this, new Block[] {}, blockX, blockY, GOLEM_BLOCK_SCALE));
    // create table of contents
    for (int i = 0; i < NUM_CONTENTS_ENTRIES; i++) {
      this.tableOfContents[i] = this.addButton(
          new GuiGolemBook.GolemEntryButton(this, ALPHABETICAL.get(i), (this.width / 2) + SCROLL_STARTX, SCR_OFFSET_Y + SCROLL_STARTY + ENTRY_H * i));
    }
    this.updateButtons();
  }

  /**
   * Called from the main game loop to update the screen.
   */
  @Override
  public void tick() {
    ++ticksOpen;
  }

  @Override
  public void render(final int mouseX, final int mouseY, final float partialTicks) {
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

    // draw background (book)
    this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
    int bookX = (this.width - BOOK_WIDTH) / 2;
    int bookY = SCR_OFFSET_Y;
    this.blit(bookX, bookY, 0, 0, BOOK_WIDTH, BOOK_HEIGHT); // drawTexturedModalRect

    // draw pages: left and right
    this.drawPageAt(bookX + 1, bookY, this.curPage, partialTicks);
    this.drawPageAt(bookX + (BOOK_WIDTH / 2) - 2, bookY, this.curPage + 1, partialTicks);

    // draw buttons, etc.
    super.render(mouseX, mouseY, partialTicks);

    // hovering text has to be the last thing you do
    if (isPageGolemEntry(this.curPage, this.totalPages) && this.buttonBlockLeft.isHovered()) {
      // hover-over on left side
      this.buttonBlockLeft.drawHoveringText(mouseX, mouseY);
    }
    if (isPageGolemEntry(this.curPage + 1, this.totalPages) && this.buttonBlockRight.isHovered()) {
      // hover-over on right side
      this.buttonBlockRight.drawHoveringText(mouseX, mouseY);
    }
  }

  /**
   * Uses the given page number to calculate which page to draw and calls the
   * appropriate methods to do so.
   *
   * @param cornerX the left corner of the page
   * @param cornerY the upper corner of the page
   * @param pageNum the page to draw
   **/
  private void drawPageAt(final int cornerX, final int cornerY, final int pageNum, final float partialTicks) {
    // draw the page number
    this.drawPageNum(cornerX, cornerY, pageNum + 1);
    // declare these for the following switch statement
    String title;
    String body;
    float scale;
    int startX;
    int startY;
    // using the page number, decides which page to draw and how to draw it
    switch (pageNum) {
    case 0:
      // draw introduction
      title = trans("item.golems.info_book");
      body = trans("golembook.intro1") + "\n" + trans("golembook.intro2");
      drawBasicPage(cornerX, cornerY, title, body);
      return;
    case 1:
      // draw Table of Contents
      title = trans("golembook.contents.title");
      drawBasicPage(cornerX, cornerY, title, "");

      // draw background
      this.getMinecraft().getTextureManager().bindTexture(CONTENTS);
      RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
      startX = cornerX + SCROLL_STARTX + 1;
      startY = cornerY + SCROLL_STARTY - 1;
      this.blit(startX, startY, 0, 0, CONTENTS_W, CONTENTS_H + 2); // drawTexturedModalRect

      // draw scroll bar
      startX = getScrollX(this.width) - 2;
      startY = getScrollY(this.currentScroll);
      this.blit(startX, startY, this.isScrolling ? (SCROLL_W + DEF_SEP) : 0, CONTENTS_H + DEF_SEP, SCROLL_W, SCROLL_H); // drawTexturedModalRect

      // update button contents
      if (this.isScrolling) {
        GolemBookEntry[] visibleArray = getGolemEntriesForScroll(this.currentScroll);
        for (int i = 0, l = this.tableOfContents.length; i < l; i++) {
          this.tableOfContents[i].setEntry(visibleArray[i]);
        }
      }
      return;
    case 2:
      // draw Golem Spell instructions
      title = TextFormatting.getTextWithoutFormattingCodes(trans("item.golems.golem_paper"));
      body = "\n\n\n\n" + I18n.format("golembook.recipe_spell.recipe", title, trans("item.minecraft.paper"), trans("item.minecraft.feather"),
          trans("item.minecraft.ink_sac"), trans("item.minecraft.redstone"));
      drawBasicPage(cornerX, cornerY, title, body);
      draw2x2GridAt(cornerX + MARGIN * 2, cornerY + MARGIN * 2, ingredientsSpell, outputSpell);
      return;
    case 3:
      // draw Golem Head instructions
      title = trans("block.golems.golem_head");
      body = "\n\n\n\n" + TextFormatting.getTextWithoutFormattingCodes(
          trans("golembook.recipe_head.recipe", title, trans("item.golems.golem_paper"), trans("block.minecraft.pumpkin")));
      drawBasicPage(cornerX, cornerY, title, body);
      draw2x2GridAt(cornerX + MARGIN * 2, cornerY + MARGIN * 2, ingredientsHead, outputHead);
      return;
    case 4:
      // draw Make Golem instructions
      title = trans("golembook.build_golem.title");
      body = trans("golembook.build_golem.howto1") + " " + trans("golembook.build_golem.howto2") + "\n\n"
          + I18n.format("golembook.build_golem.howto3", trans("block.golems.golem_head"));
      drawBasicPage(cornerX, cornerY, title, body);
      return;
    case 5:
      // draw Golem diagram
      Block golemBody = Blocks.IRON_BLOCK;
      Block golemHead = GolemItems.GOLEM_HEAD;
      scale = 2.0F;
      final int blockW = (int) (8.0F * scale);
      startX = cornerX + (BOOK_WIDTH / 8);
      startY = cornerY + blockW;
      // head
      this.drawBlock(golemHead, startX, startY, scale);
      // middle-bottom
      startY += blockW * 4;
      this.drawBlock(golemBody, startX, startY, scale);
      // arm-right
      startX += blockW * 2;
      startY -= (blockW * 5) / 2;
      this.drawBlock(golemBody, startX, startY, scale);
      // middle-top
      startX -= blockW * 2;
      startY += (blockW / 2);
      this.drawBlock(golemBody, startX, startY, scale);
      // arm-left
      startX -= blockW * 2;
      startY += (blockW / 2);
      this.drawBlock(golemBody, startX, startY, scale);
      return;
    case 6:
    default:
      // draw golem entry
      if (isPageGolemEntry(pageNum, this.totalPages)) {
        GolemBookEntry entry = getGolemEntryForPage(pageNum);
        this.drawGolemEntry(cornerX, cornerY, entry, partialTicks);
      }
      return;
    }
  }

  private void drawPageNum(final int cornerX, final int cornerY, final int pageNum) {
    boolean isLeft = pageNum % 2 == 1;
    // 'page x of xx'
    int numX = isLeft ? ((this.width / 2) - MARGIN) : ((this.width / 2) + MARGIN);
    int numY = cornerY + BOOK_HEIGHT - (ARROW_HEIGHT * 3 / 2);
    String pageNumLeft = String.valueOf(pageNum);
    int sWidth = isLeft ? this.font.getStringWidth(pageNumLeft) : 0;
    this.font.drawString(pageNumLeft, numX - sWidth, numY, 0);
  }

  /**
   * Draws the given Block in the upper-left corner of the passed page
   * coordinates.
   *
   * @param blockIn the block to draw. If this is Blocks.AIR, a barrier will be
   *                drawn instead.
   **/
  protected void drawBlock(final Block blockIn, final int cornerX, final int cornerY, final float scale) {
    // 'Blocks.AIR' is the flag for 'no block'
    Block block = blockIn != Blocks.AIR ? blockIn : Blocks.BARRIER;
    float blockX = (float) (cornerX + MARGIN + 4);
    float blockY = (float) (cornerY + MARGIN);
    // Render the Block with given scale
    RenderSystem.pushMatrix();
    RenderSystem.enableRescaleNormal();
    RenderHelper.enableStandardItemLighting();
    RenderSystem.scalef(scale, scale, scale);
    this.itemRenderer.renderItemIntoGUI(new ItemStack(block), (int) (blockX / scale), (int) (blockY / scale));
    RenderSystem.popMatrix();
  }

  /**
   * Draws the GolemEntry name and description at the given location
   **/
  private void drawGolemEntry(int cornerX, int cornerY, final GolemBookEntry entry, final float partialTicks) {
    // 'golem name' text box
    int nameX = cornerX + MARGIN * 4;
    int nameY = cornerY + MARGIN;
    String golemName = entry.getGolemName();
    this.font.drawSplitString(golemName, nameX, nameY, (BOOK_WIDTH / 2) - MARGIN * 5, 0);

    // 'golem stats' text box
    int statsX = cornerX + MARGIN;
    int statsY = nameY + MARGIN * 2;
    String stats = entry.getDescriptionPage();
    this.font.drawSplitString(stats, statsX, statsY, (BOOK_WIDTH / 2) - (MARGIN * 2), 0);

    // 'golem block'
    // this.drawBlock(entry.getBlock((int)(partialTicks * GOLEM_BLOCK_TIMER)),
    // cornerX, cornerY, GOLEM_BLOCK_SCALE);

    // 'screenshot' (supplemental image)
    if (entry.hasImage()) {
      RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
      float scale = 0.9F;
      int imgX = cornerX + (BOOK_WIDTH / 4) - (int) ((SUPP_WIDTH * scale) / 2.0F);
      int imgY = cornerY + BOOK_HEIGHT - (int) (SUPP_HEIGHT * scale) - (MARGIN * 2);
      this.getMinecraft().getTextureManager().bindTexture(entry.getImageResource());
      int w = (int) (SUPP_WIDTH * scale);
      int h = (int) (SUPP_HEIGHT * scale);
      blit(imgX, imgY, 0, 0, w, h, w, h); // drawTexturedModalRect
    }
  }

  private void drawBasicPage(int cornerX, int cornerY, String title, String body) {
    final int maxWidth = (BOOK_WIDTH / 2) - (MARGIN * 2);

    int titleX = cornerX + MARGIN + 4;
    int titleY = cornerY + MARGIN;
    int sWidth = this.font.getStringWidth(title);
    if (sWidth > maxWidth) {
      // draw title wrapped
      this.font.drawSplitString(title, titleX, titleY, maxWidth, 0);
    } else {
      // draw title centered
      this.font.drawString(title, titleX + ((maxWidth - sWidth) / 2), titleY, 0);
    }

    int bodyX = titleX;
    int bodyY = titleY + MARGIN * 2;
    this.font.drawSplitString(body, bodyX, bodyY, maxWidth, 0);
  }

  private void draw2x2GridAt(final int startX, final int startY, final ItemStack[] ingredients, final ItemStack result) {
    final int frameWidth = 3;
    final float scale = 1.0F;
    /** texture location and size of 2x2 crafting **/
    final int gridW = 84;
    final int gridH = 46;
    RenderSystem.pushMatrix();
    RenderSystem.scalef(scale, scale, scale);
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    // draw 2x2 grid background
    this.getMinecraft().getTextureManager().bindTexture(TEXTURE);
    this.blit(startX, startY, BOOK_WIDTH - gridW, BOOK_HEIGHT + DEF_SEP, gridW, gridH); // drawTexturedModalRect

    // draw itemstacks
    RenderSystem.enableRescaleNormal();
    RenderHelper.enableStandardItemLighting();
    float posX;
    float posY;
    int iconW = 15;
    switch (ingredients.length) {
    // intentional omission of break statements
    case 4:
      posX = startX + iconW + frameWidth * 3.0F;
      posY = startY + iconW + frameWidth * 3.0F;
      this.itemRenderer.renderItemIntoGUI(ingredients[3], (int) (posX / scale), (int) (posY / scale));
    case 3:
      posX = startX + frameWidth * 2.0F;
      posY = startY + iconW + frameWidth * 3.0F;
      this.itemRenderer.renderItemIntoGUI(ingredients[2], (int) (posX / scale), (int) (posY / scale));
    case 2:
      posX = startX + iconW + frameWidth * 3.0F;
      posY = startY + frameWidth * 2.0F;
      this.itemRenderer.renderItemIntoGUI(ingredients[1], (int) (posX / scale), (int) (posY / scale));
    case 1:
      posX = startX + frameWidth * 2.0F;
      posY = startY + frameWidth * 2.0F;
      this.itemRenderer.renderItemIntoGUI(ingredients[0], (int) (posX / scale), (int) (posY / scale));
    default:
      break;
    }

    // draw result itemstack
    posX = startX + gridW - 16.0F - frameWidth * 2.0F;
    posY = startY + 16.0F;
    this.itemRenderer.renderItemIntoGUI(result, (int) (posX / scale), (int) (posY / scale));

    // reset scale
    RenderSystem.popMatrix();
  }

  /**
   * Called when a mouse button is pressed and the mouse is moved around.
   * Parameters are : mouseX, mouseY, lastButtonClicked and timeSinceMouseClick.
   */
  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int state, double parDouble1, double parDouble2) {
    // detect if it's the table of contents and updates scroll button
    if (isPageTableContents(this.curPage)) {
      // if it's scrolling, center the scroll bar around the mouse
      // int scrollYCentered = getScrollY(this.currentScroll) - (SCROLL_H / 2);
      int mouseYCentered = (int) (mouseY - SCROLL_H / 2.0D);
      if (isMouseOverScroll((int) mouseX, mouseYCentered, this.width)) {
        this.isScrolling = true;
        // if the mouse Y is within the correct bounds...
        if (mouseYCentered < SCR_OFFSET_Y + SCROLL_STARTY + CONTENTS_H && mouseYCentered >= SCR_OFFSET_Y + SCROLL_STARTY) {
          // update scrolling variables
          this.currentScroll = getScrollFloat((int) (mouseY - SCROLL_H / 2.0D));
        }
      } else {
        this.isScrolling = false;
      }
    }
    return super.mouseDragged(mouseX, mouseY, state, parDouble1, parDouble2);
  }

  @Override
  public boolean mouseScrolled(double d1, double d2, double d3) {
    this.currentScroll = getScrollFloat(getScrollY(this.currentScroll + (float) d1));
    return super.mouseScrolled(d1, d2, d3);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int state) {
    this.isScrolling = false;
    return super.mouseReleased(mouseX, mouseY, state);
  }

  /**
   * Used to determine whether to show buttons
   **/
  private void updateButtons() {
    // next page arrows
    this.buttonPreviousPage.visible = this.curPage > 0;
    this.buttonNextPage.visible = this.curPage + 2 < this.totalPages;
    // table of contents buttons
    boolean tableContentsVisible = isPageTableContents(this.curPage);
    for (Button b : this.tableOfContents) {
      b.visible = tableContentsVisible;
    }
    // golem-entry block buttons
    if (isPageGolemEntry(this.curPage, this.totalPages)) {
      this.buttonBlockLeft.visible = true;
      this.buttonBlockLeft.updateBlocks(getGolemEntryForPage(this.curPage).getBlocks());
    } else {
      this.buttonBlockLeft.visible = false;
    }
    if (isPageGolemEntry(this.curPage + 1, this.totalPages)) {
      this.buttonBlockRight.visible = true;
      this.buttonBlockRight.updateBlocks(getGolemEntryForPage(this.curPage + 1).getBlocks());
    } else {
      this.buttonBlockRight.visible = false;
    }
  }

  private static boolean isMouseOverScroll(final int mouseX, final int mouseY, final int width) {
    // int currentScrollY = getScrollY(this.currentScroll);
    // check if the mouse is in the "scroll" column
    int scrollStartX = getScrollX(width);
    int scrollStartY = SCR_OFFSET_Y + SCROLL_STARTY;
    return mouseX >= scrollStartX && mouseY >= scrollStartY && mouseX < scrollStartX + SCROLL_W && mouseY < scrollStartY + CONTENTS_H - SCROLL_H / 2;
  }

  private static int getScrollX(final int screenWidth) {
    return (screenWidth / 2) + SCROLL_STARTX + CONTENTS_W - SCROLL_W;
  }

  private static int getScrollY(final float scroll) {
    // clamp scroll between 0.0 and 1.0
    final float f = Math.max(0.0F, Math.min(1.0F, scroll));
    return SCR_OFFSET_Y + SCROLL_STARTY + (int) (f * (CONTENTS_H - SCROLL_H));
  }

  private static float getScrollFloat(final int currentY) {
    final int minY = SCR_OFFSET_Y + SCROLL_STARTY;
    final int maxY = minY + CONTENTS_H - SCROLL_H;
    final float f = Math.max(minY, Math.min(maxY, currentY)) - minY;
    return (f / (CONTENTS_H - SCROLL_H));
  }

  private static boolean isPageGolemEntry(final int page, final int totalPages) {
    return page >= NUM_PAGES_INTRO && page < totalPages;
  }

  private static boolean isPageTableContents(final int page) {
    return page >= 0 && page < 2;
  }

  private static GolemBookEntry getGolemEntryForPage(final int page) {
    return GOLEMS.get(page - NUM_PAGES_INTRO);
  }

  private static GolemBookEntry[] getGolemEntriesForScroll(final float scrollIn) {
    float scroll = MathHelper.clamp(scrollIn, 0.0F, 1.0F);
    int i = (int) (scroll * (float) (ALPHABETICAL.size() - NUM_CONTENTS_ENTRIES));
    return ALPHABETICAL.subList(i, i + NUM_CONTENTS_ENTRIES).toArray(new GolemBookEntry[NUM_CONTENTS_ENTRIES]);
  }

  /**
   * Helper method for translating text into local language using {@code I18n}
   **/
  protected static String trans(final String s, final Object... strings) {
    return I18n.format(s, strings);
  }

  protected static class BlockButton extends Button {

    private float scale;
    private Block[] blocks;
    private Block currentBlock;
    private final GuiGolemBook gui;

    public BlockButton(GuiGolemBook guiIn, Block[] blockValues, int x, int y, float scaleIn) {
      super(x, y, (int) (scaleIn * 16.0F), (int) (scaleIn * 16.0F), "", b -> {
      });
      this.gui = guiIn;
      this.blocks = blockValues;
      this.scale = scaleIn;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
      if (this.visible) {
        // update hovered flag
        this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        // update the block to draw
        if (blocks != null && blocks.length > 0) {
          int index = (int) (gui.ticksOpen / 30) % blocks.length;
          this.currentBlock = this.blocks[index];
        } else {
          this.currentBlock = Blocks.AIR;
        }
        // draw the block
        gui.drawBlock(this.currentBlock, this.x - MARGIN - 4, this.y - MARGIN, this.scale);
      }
    }

    public void updateBlocks(final Block[] blocksToDraw) {
      this.blocks = blocksToDraw;
    }

    /**
     * Draws the name of the current block as a hovering text. Exception: draws
     * nothing if current block is Blocks.AIR
     *
     * @return if the text was successfully drawn
     **/
    public boolean drawHoveringText(final int mouseX, final int mouseY) {
      // draw the name of the block if this button is being hovered over
      if (this.currentBlock != Blocks.AIR) {
        this.gui.renderTooltip(trans(this.currentBlock.getTranslationKey()), mouseX, mouseY);
        return true;
      }
      return false;
    }
  }

  protected static class GolemEntryButton extends Button {

    private final GuiGolemBook gui;
    private GolemBookEntry entry;

    public GolemEntryButton(final GuiGolemBook guiIn, final GolemBookEntry entryIn, final int x, final int y) {
      super(x, y, ENTRY_W, ENTRY_H, "", b -> {
      });
      this.gui = guiIn;
      this.entry = entryIn;
    }

    public void setEntry(GolemBookEntry toSet) {
      this.entry = toSet;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
      int index = GOLEMS.indexOf(this.entry);
      if (index >= 0 && index <= GOLEMS.size()) {
        int page = index + NUM_PAGES_INTRO;
        // make sure it's an even number
        page = Math.floorDiv(page, 2) * 2;
        this.gui.curPage = page;
      }
      this.gui.updateButtons();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
      if (this.visible) {
        this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        Minecraft.getInstance().getTextureManager().bindTexture(CONTENTS);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        // draw the background of the button
        this.blit(this.x, this.y, CONTENTS_W + DEF_SEP, this.isHovered ? (ENTRY_H + DEF_SEP) : 0, ENTRY_W, ENTRY_H); // drawTexturedModalRect
        // draw the block and name of the golem
        int index = (int) (gui.ticksOpen / 30);
        gui.drawBlock(this.entry.getBlock(index), this.x - MARGIN - 2, this.y - 9, 1.0F);

        // prepare to draw the golem's name
        RenderSystem.pushMatrix();

        final String name = entry.getGolemName();
        final int wrap = this.width - 20;
        float scale = 1.0F;
        int nameH = gui.font.getWordWrappedHeight(name, wrap);
        if (nameH > this.height) {
          scale = 0.7F;
          nameH = (int) (scale * gui.font.getWordWrappedHeight(name, (int) (wrap / scale)));
        }
        int nameX = this.x + 20;
        int nameY = this.y + ((this.height - nameH) / 2) + 1;
        // re-scale and draw the golem name
        RenderSystem.scalef(scale, scale, scale);
        gui.font.drawSplitString(name, (int) ((nameX) / scale), (int) (nameY / scale), (int) (wrap / scale), 0);
        RenderSystem.popMatrix();
      }
    }
  }

  protected static class NextPageButton extends Button {

    // texture starts at this location
    protected static final int TEXTURE_STARTY = BOOK_HEIGHT + DEF_SEP;
    private final GuiGolemBook gui;
    private final boolean isForward;

    public NextPageButton(GuiGolemBook guiBook, int x, int y, boolean isForwardIn) {
      super(x, y, ARROW_WIDTH, ARROW_HEIGHT, "", b -> {
      });
      this.gui = guiBook;
      this.isForward = isForwardIn;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
      if (this.isForward) {
        this.gui.curPage += 2;
      } else {
        this.gui.curPage -= 2;
      }
      this.gui.updateButtons();
    }

    /**
     * Draws this button to the screen.
     */
    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {

      if (this.visible) {
        boolean mouseOver = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bindTexture(GuiGolemBook.TEXTURE);
        int tx = 0;
        int ty = TEXTURE_STARTY;

        if (mouseOver) {
          tx += ARROW_WIDTH + DEF_SEP;
        }

        if (!this.isForward) {
          ty += ARROW_HEIGHT;
        }

        this.blit(this.x, this.y, tx, ty, ARROW_WIDTH, ARROW_HEIGHT); // drawTexturedModalRect
      }
    }
  }
}
