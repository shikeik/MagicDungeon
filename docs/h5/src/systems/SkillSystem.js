export const SKILLS = {
    HEAL: { id: 'heal', name: 'Heal', desc: 'Heal 20 HP', cost: 1, cooldown: 5, effect: (player) => {
        player.stats.hp = Math.min(player.stats.maxHp, player.stats.hp + 20);
        return true;
    }},
    RAGE: { id: 'rage', name: 'Rage', desc: 'Double damage for 3 turns', cost: 2, cooldown: 10, effect: (player) => {
        // Implement status effect logic later
        console.log("Rage activated!");
        return true;
    }}
};

export class SkillSystem {
    constructor(player) {
        this.player = player;
        this.unlockedSkills = {}; // id -> cooldown timer
    }

    unlock(skillId) {
        if (SKILLS[skillId] && this.player.stats.skillPoints >= SKILLS[skillId].cost) {
            this.player.stats.skillPoints -= SKILLS[skillId].cost;
            this.unlockedSkills[skillId] = 0; // 0 cooldown
            return true;
        }
        return false;
    }

    use(skillId) {
        if (this.unlockedSkills[skillId] !== undefined && this.unlockedSkills[skillId] <= 0) {
            const skill = SKILLS[skillId];
            if (skill.effect(this.player)) {
                this.unlockedSkills[skillId] = skill.cooldown;
                return true;
            }
        }
        return false;
    }

    update(dt) {
        for (const id in this.unlockedSkills) {
            if (this.unlockedSkills[id] > 0) {
                this.unlockedSkills[id] -= dt;
            }
        }
    }
}
