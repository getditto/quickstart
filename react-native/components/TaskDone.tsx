import BouncyCheckbox from "react-native-bouncy-checkbox";

type Props = {
    checked: boolean,
    onPress: () => void,
}

const TaskDone: React.FC<Props> = ({ checked, onPress }) => {
    return (
        <BouncyCheckbox
            isChecked={checked}
            onPress={onPress}
            fillColor="#7C3AED"
            useBuiltInState={false}
        />
    );
};

export default TaskDone;
