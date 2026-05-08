@Mixin(Entity.class)
public abstract class PlayerPositionMixin {
    @Inject(method = "m_20191_", at = @At("RETURN"), cancellable = true, remap = false)
    private void zerog$onGetBoundingBox(CallbackInfoReturnable<AABB> cir) {
        if ((Object)this instanceof Player player) {
             // 如果在零重力下，可以返回一个以玩家为中心的正方体 AABB
             // 这能解决侧着身子进不去门或者卡墙的问题
        }
    }
}
