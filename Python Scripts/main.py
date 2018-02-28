import sys
import getopt
import matplotlib.pyplot as plt


network_activity = []
screen_activity = []
battery_activity = []
charge_activity = []

first_timestamp = 0
last_rx_bytes = 0
last_tx_bytes = 0


def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:], "i:o:n:", ["input=", "output="])
    except getopt.GetoptError as err:
        print(err)
        sys.exit(2)
    input_file = None
    output_file = None
    lines = 0
    for op, arg in opts:
        if op in ("-i", "--input"):
            input_file = arg
        elif op in ("-o", "--output"):
            output_file = arg
        elif op == "-n":
            lines = int(arg)
        else:
            assert False, "unrecognized argument"

    if input_file is None or output_file is None:
        assert False, "missing input arguments"

    parse_data(input_file, output_file, lines)

    # for log in network_activity:
    #     print(f"{log['time']} - Bytes Received: {log['bytes_in']} - Bytes Sent: {log['bytes_out']}")
    #
    # for log in screen_activity:
    #     print(f"{log['time']} - Screen {log['status']}")
    #
    # for log in battery_activity:
    #     print(f"{log['time']} - Battery Level {log['level']}")

    plot_data()


def parse_data(input_file, output_file, lines_to_parse=0):
    file = open(input_file, "r")
    counter = lines_to_parse

    for line in file:
        parse_line(line)
        counter -= 1
        if counter == 0:
            break

    file.close()


def parse_line(line):
    global first_timestamp

    values = line.split(";")
    timestamp = int(values[1])
    log = values[3]
    argument = values[4].strip()
    log_values = log.split("|")

    if first_timestamp == 0:
        first_timestamp = timestamp

    timestamp -= first_timestamp

    if log == "screen|power":
        log_screen_activity(timestamp, argument)

    if log == "net|total|rx_bytes":
        log_network_activity_rx(timestamp, argument)

    if log == "net|total|tx_bytes":
        log_network_activity_tx(timestamp, argument)

    if log == "power|battery|level":
        log_battery_level(timestamp, argument)


def log_network_activity_rx(time, argument):
    global last_rx_bytes

    bytes_transferred = int(argument)
    if last_rx_bytes == 0:
        last_rx_bytes = bytes_transferred
    else:
        bytes_transferred -= last_rx_bytes
        net_activity = {"time": time, "bytes_in": bytes_transferred, "bytes_out": 0}

        flag = False
        if len(network_activity) > 0:
            last_item = network_activity[-1]
            if last_item['time'] == time:
                last_item['bytes_in'] += net_activity['bytes_in']
                last_item['bytes_out'] += net_activity['bytes_out']
                flag = True

        if not flag:
            network_activity.append(net_activity)

        last_rx_bytes += bytes_transferred


def log_network_activity_tx(time, argument):
    global last_tx_bytes

    bytes_transferred = int(argument)
    if last_tx_bytes == 0:
        last_tx_bytes = bytes_transferred
    else:
        bytes_transferred -= last_tx_bytes
        net_activity = {"time": time, "bytes_in": 0, "bytes_out": bytes_transferred}

        flag = False
        if len(network_activity) > 0:
            last_item = network_activity[-1]
            if last_item['time'] == time:
                last_item['bytes_in'] += net_activity['bytes_in']
                last_item['bytes_out'] += net_activity['bytes_out']
                flag = True

        if not flag:
            network_activity.append(net_activity)

        last_tx_bytes += bytes_transferred


def log_screen_activity(time, argument):
    if argument == "on":
        screen_activity.append({"time": time, "status": "ON"})
    else:
        screen_activity.append({"time": time, "status": "OFF"})


def log_battery_level(time, argument):
    battery_level = int(argument)
    battery_activity.append({"time": time, "level": battery_level})


def plot_data():
    battery_x = []
    battery_y = []

    for battery_level in battery_activity:
        time = battery_level['time'] / 1000
        battery_x.append(time)
        battery_y.append(battery_level['level'])

    values = []
    heights = []
    widths = []

    last_time = -1
    for net_data in network_activity:
        print(f"{net_data['time']} - Bytes Received: {net_data['bytes_in']} - Bytes Sent: {net_data['bytes_out']}")
        time = net_data['time'] / 1000
        if last_time == -1:
            values.append(time / 2)
            widths.append(time)
        else:
            values.append((time + last_time) / 2)
            widths.append(time - last_time)
        heights.append((net_data['bytes_in'] + net_data['bytes_out']) / 1024)
        last_time = time

    fig, ax1 = plt.subplots()
    ax1.bar(values, height=heights, width=widths)
    ax1.set_xlabel("Time (s)")
    ax1.set_ylabel("Bytes Transferred (KB)")

    ax2 = ax1.twinx()
    ax2.plot(battery_x, battery_y, color='red')
    axis = ax2.axis()
    ax2.axis([axis[0], axis[1], 0, 100])
    ax2.set_ylabel("Battery State of Charge")

    fig.tight_layout()
    plt.show()


if __name__ == "__main__":
    main()
