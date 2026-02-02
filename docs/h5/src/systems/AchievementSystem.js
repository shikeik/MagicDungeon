export const ACHIEVEMENTS = [
    { id: 'first_blood', name: 'First Blood', desc: 'Kill your first monster', condition: (stats) => stats.kills >= 1 },
    { id: 'hunter', name: 'Hunter', desc: 'Kill 10 monsters', condition: (stats) => stats.kills >= 10 },
    { id: 'survivor', name: 'Survivor', desc: 'Reach Level 5', condition: (stats) => stats.level >= 5 }
];

export class AchievementSystem {
    constructor() {
        this.unlocked = new Set();
    }

    check(stats) {
        const newUnlocks = [];
        ACHIEVEMENTS.forEach(ach => {
            if (!this.unlocked.has(ach.id) && ach.condition(stats)) {
                this.unlocked.add(ach.id);
                newUnlocks.push(ach);
                console.log(`Achievement Unlocked: ${ach.name}`);
            }
        });
        return newUnlocks;
    }
}
